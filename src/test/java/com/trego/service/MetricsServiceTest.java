package com.trego.service;

import com.trego.dto.MetricsSnapshotDto;
import com.trego.dto.PrEntryDto;
import com.trego.dto.WeeklyMetricsDto;
import com.trego.dto.RecomputeResultDto;
import com.trego.model.RunRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceTest {
    static final String UID = "test-user";
    static final ZoneOffset UTC = ZoneOffset.UTC;

    InMemoryMetricsRepository repo;
    MetricsService service;
    Clock fixedClock;

    @BeforeEach
    void setUp() {
        repo = new InMemoryMetricsRepository();
        // Fix "now" at Tuesday 2026-04-21 12:00:00 UTC, mid-ISO-week 2026-W17
        fixedClock = Clock.fixed(Instant.parse("2026-04-21T12:00:00Z"), UTC);
        service = new MetricsService(repo, fixedClock);
    }

    private RunRecord run(String id, String startIso, double km, long durationMs) {
        return new RunRecord(id, km, durationMs, Instant.parse(startIso));
    }

    @Test
    void emptyRunListProducesZeroFilledSnapshot() {
        RecomputeResultDto result = service.recompute(UID);

        assertEquals(0, result.getRunCount());
        var stored = repo.snapshotsByUser.get(UID);
        assertNotNull(stored);
        MetricsSnapshotDto dto = stored.dto;
        assertEquals(0.0, dto.getTotals().getTotalKm());
        assertEquals(0, dto.getTotals().getTotalRuns());
        assertEquals(0, dto.getThisWeek().getTotalRuns());
        assertNull(dto.getPrs().getFastest1k());
        assertNull(dto.getPrs().getFastest5k());
        assertNull(dto.getPrs().getFastest10k());
        assertNull(dto.getPrs().getLongestDistance());
        assertNull(dto.getPrs().getLongestDuration());
        assertEquals(12, dto.getHistory().size(), "history should always have 12 entries");
        // current week is the last entry
        assertEquals("2026-W17", dto.getHistory().get(11).getIsoYearWeek());
    }

    @Test
    void singleRunPopulatesEverything() {
        // 5.2 km in 24:13 (1453000 ms), Tuesday 2026-04-21 08:04 UTC
        repo.runsByUser.put(UID, List.of(
                run("r1", "2026-04-21T08:04:00Z", 5.2, 24 * 60 * 1000 + 13 * 1000)
        ));

        service.recompute(UID);

        MetricsSnapshotDto dto = repo.snapshotsByUser.get(UID).dto;
        assertEquals(5.2, dto.getTotals().getTotalKm(), 0.0001);
        assertEquals(1, dto.getTotals().getTotalRuns());
        assertEquals(1, dto.getThisWeek().getTotalRuns());
        assertEquals("2026-W17", dto.getThisWeek().getIsoYearWeek());

        // 5.2 km qualifies for fastest1k AND fastest5k. Pace = 1453s/5.2km ≈ 279 s/km.
        assertNotNull(dto.getPrs().getFastest1k());
        assertNotNull(dto.getPrs().getFastest5k());
        assertNull(dto.getPrs().getFastest10k(), "no run >= 10K");
        assertNotNull(dto.getPrs().getLongestDistance());
        assertNotNull(dto.getPrs().getLongestDuration());
        assertEquals(279L, dto.getPrs().getFastest5k().getPaceSecPerKm());
    }

    @Test
    void fasterRunReplacesPr() {
        // First run: 5K in 30:00 (slow). Second: 5K in 25:00 (faster).
        repo.runsByUser.put(UID, List.of(
                run("slow", "2026-03-01T08:00:00Z", 5.0, 30 * 60 * 1000),
                run("fast", "2026-03-08T08:00:00Z", 5.0, 25 * 60 * 1000)
        ));

        service.recompute(UID);

        PrEntryDto fastest5k = repo.snapshotsByUser.get(UID).dto.getPrs().getFastest5k();
        assertEquals("fast", fastest5k.getRunId());
        assertEquals(300L, fastest5k.getPaceSecPerKm());  // 25*60/5 = 300
    }

    @Test
    void longerRunReplacesLongestPr() {
        repo.runsByUser.put(UID, List.of(
                run("short", "2026-03-01T08:00:00Z", 5.0, 30 * 60 * 1000),
                run("long",  "2026-03-08T08:00:00Z", 12.0, 70 * 60 * 1000)
        ));

        service.recompute(UID);

        var prs = repo.snapshotsByUser.get(UID).dto.getPrs();
        assertEquals("long", prs.getLongestDistance().getRunId());
        assertEquals(12.0, prs.getLongestDistance().getDistanceKm(), 0.0001);
        assertEquals("long", prs.getLongestDuration().getRunId());
    }

    @Test
    void fastest10kRequiresAtLeast10kRun() {
        // 8K run — should NOT populate fastest10k.
        repo.runsByUser.put(UID, List.of(
                run("eight", "2026-03-01T08:00:00Z", 8.0, 40 * 60 * 1000)
        ));

        service.recompute(UID);

        assertNull(repo.snapshotsByUser.get(UID).dto.getPrs().getFastest10k());
    }

    @Test
    void streakCountsConsecutiveDaysAnchoredAtToday() {
        // Today: Tuesday 2026-04-21. Run on today, yesterday, day-before. Streak = 3.
        repo.runsByUser.put(UID, List.of(
                run("d-2", "2026-04-19T08:00:00Z", 3.0, 18 * 60 * 1000),  // Sunday
                run("d-1", "2026-04-20T08:00:00Z", 3.0, 18 * 60 * 1000),  // Monday
                run("d-0", "2026-04-21T08:00:00Z", 3.0, 18 * 60 * 1000)   // Tuesday — today
        ));

        service.recompute(UID);

        assertEquals(3, repo.snapshotsByUser.get(UID).dto.getThisWeek().getStreakDays());
    }

    @Test
    void streakBreaksOnGap() {
        // Today: Tuesday. Yesterday rest day. Streak = 1 (today only).
        repo.runsByUser.put(UID, List.of(
                run("d-2", "2026-04-19T08:00:00Z", 3.0, 18 * 60 * 1000),  // Sunday
                run("d-0", "2026-04-21T08:00:00Z", 3.0, 18 * 60 * 1000)   // Tuesday — today
        ));

        service.recompute(UID);

        assertEquals(1, repo.snapshotsByUser.get(UID).dto.getThisWeek().getStreakDays());
    }

    @Test
    void streakIsZeroWhenNoRunToday() {
        // Today: Tuesday. Most recent run was yesterday. Streak = 0 (must include today).
        repo.runsByUser.put(UID, List.of(
                run("d-1", "2026-04-20T08:00:00Z", 3.0, 18 * 60 * 1000)
        ));

        service.recompute(UID);

        assertEquals(0, repo.snapshotsByUser.get(UID).dto.getThisWeek().getStreakDays());
    }

    @Test
    void historyZeroFillsWeeksWithNoRuns() {
        // Single run 8 weeks ago. Other 11 weeks should be zero-filled.
        repo.runsByUser.put(UID, List.of(
                run("old", "2026-02-28T08:00:00Z", 5.0, 25 * 60 * 1000)
        ));

        service.recompute(UID);

        List<WeeklyMetricsDto> hist = repo.snapshotsByUser.get(UID).dto.getHistory();
        assertEquals(12, hist.size());
        long nonZeroWeeks = hist.stream().filter(w -> w.getTotalRuns() > 0).count();
        assertEquals(1, nonZeroWeeks);
    }

    @Test
    void multipleRunsSameDayCollapseToSingleDailyAggregate() {
        repo.runsByUser.put(UID, List.of(
                run("am", "2026-04-21T08:00:00Z", 3.0, 18 * 60 * 1000),
                run("pm", "2026-04-21T18:00:00Z", 5.0, 30 * 60 * 1000)
        ));

        service.recompute(UID);

        var daily = repo.dailyByUser.get(UID);
        assertEquals(1, daily.size());
        var d = daily.get(LocalDate.parse("2026-04-21"));
        assertEquals(8.0, d.totalKm, 0.0001);
        assertEquals(2, d.totalRuns);
    }

    @Test
    void isoWeekStraddlingYearBoundary() {
        // 2025-12-29 is Monday of ISO week 2026-W01 (because ISO weeks have Thursday in Jan).
        // The week-based-year here is 2026.
        repo.runsByUser.put(UID, List.of(
                run("ny", "2025-12-29T10:00:00Z", 5.0, 25 * 60 * 1000)
        ));

        // Override the clock so that this run falls inside the 12-week window.
        Clock laterClock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), UTC);
        service = new MetricsService(repo, laterClock);

        service.recompute(UID);

        List<WeeklyMetricsDto> hist = repo.snapshotsByUser.get(UID).dto.getHistory();
        assertTrue(hist.stream().anyMatch(w -> w.getIsoYearWeek().equals("2026-W01")));
    }

    @Test
    void runCountAtComputeMatchesActualCount() {
        repo.runsByUser.put(UID, List.of(
                run("a", "2026-04-21T08:00:00Z", 3.0, 18 * 60 * 1000),
                run("b", "2026-04-22T08:00:00Z", 3.0, 18 * 60 * 1000)
        ));

        service.recompute(UID);

        assertEquals(2, repo.snapshotsByUser.get(UID).runCountAtCompute);
    }

    @Test
    void recomputeIsIdempotent() {
        repo.runsByUser.put(UID, List.of(
                run("a", "2026-04-21T08:00:00Z", 5.0, 25 * 60 * 1000)
        ));

        service.recompute(UID);
        var first = repo.snapshotsByUser.get(UID).dto;

        service.recompute(UID);
        var second = repo.snapshotsByUser.get(UID).dto;

        // Same totals, PRs, history. computedAt may differ — exclude from comparison.
        assertEquals(first.getTotals().getTotalKm(), second.getTotals().getTotalKm());
        assertEquals(first.getPrs().getFastest5k().getRunId(),
                     second.getPrs().getFastest5k().getRunId());
        assertEquals(first.getHistory().size(), second.getHistory().size());
    }

    @Test
    void getGoalReturnsEmptyWhenUnset() {
        var goal = service.getGoal(UID);
        assertNull(goal.getTargetKm());
        assertNull(goal.getTargetRuns());
    }

    @Test
    void setGoalPersistsAndStampsUpdatedAt() {
        var saved = service.setGoal(UID, new com.trego.dto.WeeklyGoalDto(25.0, 4, null));
        assertEquals(25.0, saved.getTargetKm());
        assertEquals(4, saved.getTargetRuns());
        // Clock is fixed at 2026-04-21T12:00:00Z in setUp.
        assertEquals(Instant.parse("2026-04-21T12:00:00Z"), saved.getUpdatedAt());

        var read = service.getGoal(UID);
        assertEquals(25.0, read.getTargetKm());
        assertEquals(4, read.getTargetRuns());
    }

    @Test
    void setGoalAllowsPartialTargets() {
        var saved = service.setGoal(UID, new com.trego.dto.WeeklyGoalDto(30.0, null, null));
        assertEquals(30.0, saved.getTargetKm());
        assertNull(saved.getTargetRuns());
    }
}
