package com.trego.service;

import com.trego.dto.*;
import com.trego.model.RunRecord;
import com.trego.repository.MetricsRepository;
import com.trego.repository.MetricsRepository.DailyAggregate;
import com.trego.repository.MetricsRepository.StoredSnapshot;

import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class MetricsService {
    public static final int SCHEMA_VERSION = 1;
    public static final int HISTORY_WEEKS = 12;
    public static final Duration STALE_AFTER = Duration.ofHours(24);

    private final MetricsRepository repo;
    private final Clock clock;

    /** Spring constructor — uses system clock. */
    public MetricsService(MetricsRepository repo) {
        this(repo, Clock.systemUTC());
    }

    /** Test constructor with injectable clock. */
    public MetricsService(MetricsRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /**
     * Read the user's weekly goal, or an empty (all-null) goal if none is set.
     */
    public WeeklyGoalDto getGoal(String uid) {
        return repo.readGoal(uid).orElseGet(() -> new WeeklyGoalDto(null, null, null));
    }

    /**
     * Persist the user's weekly goal, stamping updatedAt. Returns the stored goal.
     */
    public WeeklyGoalDto setGoal(String uid, WeeklyGoalDto goal) {
        WeeklyGoalDto toStore = new WeeklyGoalDto(
                goal.getTargetKm(), goal.getTargetRuns(), Instant.now(clock));
        repo.writeGoal(uid, toStore);
        return toStore;
    }

    /**
     * Read the current snapshot, recomputing if missing or stale.
     */
    public MetricsSnapshotDto getSnapshot(String uid) {
        Optional<StoredSnapshot> stored = repo.readSnapshot(uid);
        if (stored.isEmpty() || isStale(stored.get(), uid)) {
            recompute(uid);
            stored = repo.readSnapshot(uid);
        }
        return stored.get().dto;
    }

    /**
     * Recompute all metrics for a user. Idempotent.
     */
    public RecomputeResultDto recompute(String uid) {
        long started = System.currentTimeMillis();
        List<RunRecord> runs = repo.readRuns(uid);

        // Accumulators
        PrsDto prs = new PrsDto();
        LifetimeTotalsDto totals = new LifetimeTotalsDto();
        Map<LocalDate, DailyAggregate> dailyByDate = new HashMap<>();
        Map<String, WeeklyMetricsDto> weeklyByIsoWeek = new HashMap<>();

        for (RunRecord run : runs) {
            long paceSecPerKm = run.getDistanceKm() > 0
                    ? Math.round((run.getDurationMs() / 1000.0) / run.getDistanceKm())
                    : Long.MAX_VALUE;

            // PRs
            if (run.getDistanceKm() >= 1.0) {
                if (prs.getFastest1k() == null || paceSecPerKm < prs.getFastest1k().getPaceSecPerKm()) {
                    prs.setFastest1k(makePacePr(run, paceSecPerKm));
                }
            }
            if (run.getDistanceKm() >= 5.0) {
                if (prs.getFastest5k() == null || paceSecPerKm < prs.getFastest5k().getPaceSecPerKm()) {
                    prs.setFastest5k(makePacePr(run, paceSecPerKm));
                }
            }
            if (run.getDistanceKm() >= 10.0) {
                if (prs.getFastest10k() == null || paceSecPerKm < prs.getFastest10k().getPaceSecPerKm()) {
                    prs.setFastest10k(makePacePr(run, paceSecPerKm));
                }
            }
            if (prs.getLongestDistance() == null || run.getDistanceKm() > prs.getLongestDistance().getDistanceKm()) {
                prs.setLongestDistance(makeDistancePr(run));
            }
            if (prs.getLongestDuration() == null || run.getDurationMs() > prs.getLongestDuration().getDurationMs()) {
                prs.setLongestDuration(makeDurationPr(run));
            }

            // Totals
            totals.setTotalKm(totals.getTotalKm() + run.getDistanceKm());
            totals.setTotalRuns(totals.getTotalRuns() + 1);
            totals.setTotalTimeMs(totals.getTotalTimeMs() + run.getDurationMs());

            // Daily aggregates
            LocalDate date = run.getStartTime().atZone(ZoneOffset.UTC).toLocalDate();
            DailyAggregate prev = dailyByDate.get(date);
            if (prev == null) {
                dailyByDate.put(date, new DailyAggregate(run.getDistanceKm(), 1, run.getDurationMs()));
            } else {
                dailyByDate.put(date, new DailyAggregate(
                        prev.totalKm + run.getDistanceKm(),
                        prev.totalRuns + 1,
                        prev.totalTimeMs + run.getDurationMs()
                ));
            }

            // Weekly aggregates
            String isoWeek = isoYearWeekOf(run.getStartTime());
            WeeklyMetricsDto wk = weeklyByIsoWeek.computeIfAbsent(isoWeek, k -> {
                WeeklyMetricsDto w = new WeeklyMetricsDto();
                w.setIsoYearWeek(k);
                Instant[] bounds = isoWeekBoundsUtc(run.getStartTime());
                w.setWeekStart(bounds[0]);
                w.setWeekEnd(bounds[1]);
                return w;
            });
            wk.setTotalKm(wk.getTotalKm() + run.getDistanceKm());
            wk.setTotalRuns(wk.getTotalRuns() + 1);
            wk.setTotalTimeMs(wk.getTotalTimeMs() + run.getDurationMs());
            if (run.getDistanceKm() > wk.getLongestKm()) wk.setLongestKm(run.getDistanceKm());
        }

        // avgPaceSecPerKm fill-in for each weekly bucket
        for (WeeklyMetricsDto w : weeklyByIsoWeek.values()) {
            w.setAvgPaceSecPerKm(w.getTotalKm() > 0
                    ? Math.round((w.getTotalTimeMs() / 1000.0) / w.getTotalKm())
                    : 0L);
        }

        // Build snapshot
        Instant now = Instant.now(clock);
        String currentIsoWeek = isoYearWeekOf(now);
        WeeklyMetricsDto thisWeek = weeklyByIsoWeek.getOrDefault(currentIsoWeek,
                emptyWeek(currentIsoWeek, isoWeekBoundsUtc(now)));
        thisWeek.setStreakDays(computeStreak(dailyByDate, now));

        List<WeeklyMetricsDto> history = buildHistory(weeklyByIsoWeek, now);

        MetricsSnapshotDto dto = new MetricsSnapshotDto();
        dto.setComputedAt(now);
        dto.setThisWeek(thisWeek);
        dto.setPrs(prs);
        dto.setTotals(totals);
        dto.setHistory(history);

        // Persist
        StoredSnapshot stored = new StoredSnapshot(SCHEMA_VERSION, runs.size(), dto);
        repo.writeSnapshot(uid, stored, dailyByDate);

        long duration = System.currentTimeMillis() - started;
        return new RecomputeResultDto(now, runs.size(), duration);
    }

    // --- helpers ---

    private boolean isStale(StoredSnapshot stored, String uid) {
        if (stored.schemaVersion != SCHEMA_VERSION) return true;
        if (stored.runCountAtCompute != repo.countRuns(uid)) return true;
        if (stored.dto.getComputedAt() == null) return true;
        return Instant.now(clock).isAfter(stored.dto.getComputedAt().plus(STALE_AFTER));
    }

    private static String isoYearWeekOf(Instant t) {
        ZonedDateTime z = t.atZone(ZoneOffset.UTC);
        int weekBasedYear = z.get(WeekFields.ISO.weekBasedYear());
        int weekOfYear = z.get(WeekFields.ISO.weekOfWeekBasedYear());
        return String.format("%d-W%02d", weekBasedYear, weekOfYear);
    }

    private static Instant[] isoWeekBoundsUtc(Instant inWeek) {
        ZonedDateTime z = inWeek.atZone(ZoneOffset.UTC);
        ZonedDateTime monday = z.with(java.time.DayOfWeek.MONDAY).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        ZonedDateTime sundayEnd = monday.plusDays(7).minusNanos(1);
        return new Instant[]{ monday.toInstant(), sundayEnd.toInstant() };
    }

    private static WeeklyMetricsDto emptyWeek(String isoWeek, Instant[] bounds) {
        WeeklyMetricsDto w = new WeeklyMetricsDto();
        w.setIsoYearWeek(isoWeek);
        w.setWeekStart(bounds[0]);
        w.setWeekEnd(bounds[1]);
        return w;
    }

    private static int computeStreak(Map<LocalDate, DailyAggregate> daily, Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (!daily.containsKey(today)) return 0;
        int streak = 1;
        LocalDate cursor = today.minusDays(1);
        while (daily.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static List<WeeklyMetricsDto> buildHistory(Map<String, WeeklyMetricsDto> weeklyByIsoWeek, Instant now) {
        List<WeeklyMetricsDto> history = new ArrayList<>(HISTORY_WEEKS);
        ZonedDateTime cursor = now.atZone(ZoneOffset.UTC);
        // Walk back HISTORY_WEEKS - 1 weeks, oldest first
        for (int i = HISTORY_WEEKS - 1; i >= 0; i--) {
            ZonedDateTime weekTime = cursor.minusWeeks(i);
            Instant inThatWeek = weekTime.toInstant();
            String isoWeek = isoYearWeekOf(inThatWeek);
            WeeklyMetricsDto bucket = weeklyByIsoWeek.get(isoWeek);
            if (bucket != null) {
                WeeklyMetricsDto copy = cloneWeek(bucket);
                copy.setStreakDays(0);
                history.add(copy);
            } else {
                history.add(emptyWeek(isoWeek, isoWeekBoundsUtc(inThatWeek)));
            }
        }
        return history;
    }

    private static WeeklyMetricsDto cloneWeek(WeeklyMetricsDto src) {
        WeeklyMetricsDto c = new WeeklyMetricsDto();
        c.setIsoYearWeek(src.getIsoYearWeek());
        c.setWeekStart(src.getWeekStart());
        c.setWeekEnd(src.getWeekEnd());
        c.setTotalKm(src.getTotalKm());
        c.setTotalRuns(src.getTotalRuns());
        c.setTotalTimeMs(src.getTotalTimeMs());
        c.setAvgPaceSecPerKm(src.getAvgPaceSecPerKm());
        c.setLongestKm(src.getLongestKm());
        c.setStreakDays(src.getStreakDays());
        return c;
    }

    private static PrEntryDto makePacePr(RunRecord run, long paceSecPerKm) {
        PrEntryDto p = new PrEntryDto();
        p.setRunId(run.getRunId());
        p.setDistanceKm(run.getDistanceKm());
        p.setRunStartTime(run.getStartTime());
        p.setPaceSecPerKm(paceSecPerKm);
        return p;
    }

    private static PrEntryDto makeDistancePr(RunRecord run) {
        PrEntryDto p = new PrEntryDto();
        p.setRunId(run.getRunId());
        p.setDistanceKm(run.getDistanceKm());
        p.setRunStartTime(run.getStartTime());
        p.setDurationMs(run.getDurationMs());
        return p;
    }

    private static PrEntryDto makeDurationPr(RunRecord run) {
        PrEntryDto p = new PrEntryDto();
        p.setRunId(run.getRunId());
        p.setDistanceKm(run.getDistanceKm());
        p.setRunStartTime(run.getStartTime());
        p.setDurationMs(run.getDurationMs());
        return p;
    }
}
