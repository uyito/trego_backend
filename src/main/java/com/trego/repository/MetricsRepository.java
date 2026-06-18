package com.trego.repository;

import com.trego.dto.MetricsSnapshotDto;
import com.trego.dto.WeeklyGoalDto;
import com.trego.model.RunRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage abstraction for metrics.
 * Production impl: FirestoreMetricsRepository (Task 4).
 * Test impl: InMemoryMetricsRepository (this task).
 */
public interface MetricsRepository {
    /** Return all runs for a user, ordered by startTime ASC. */
    List<RunRecord> readRuns(String uid);

    /** Count of runs in users/{uid}/runs/. Used for runCountAtCompute sanity check. */
    int countRuns(String uid);

    /** Read materialized snapshot, or empty if absent. */
    Optional<StoredSnapshot> readSnapshot(String uid);

    /** Write snapshot doc + per-day aggregate docs. */
    void writeSnapshot(String uid, StoredSnapshot snapshot, Map<LocalDate, DailyAggregate> dailyByDate);

    /** Read the user's weekly goal, or empty if none set. */
    Optional<WeeklyGoalDto> readGoal(String uid);

    /** Persist the user's weekly goal. */
    void writeGoal(String uid, WeeklyGoalDto goal);

    /** Container for the persisted snapshot — schema version + run count + the public DTO. */
    final class StoredSnapshot {
        public final int schemaVersion;
        public final int runCountAtCompute;
        public final MetricsSnapshotDto dto;

        public StoredSnapshot(int schemaVersion, int runCountAtCompute, MetricsSnapshotDto dto) {
            this.schemaVersion = schemaVersion;
            this.runCountAtCompute = runCountAtCompute;
            this.dto = dto;
        }
    }

    /** One day's totals. */
    final class DailyAggregate {
        public final double totalKm;
        public final int totalRuns;
        public final long totalTimeMs;

        public DailyAggregate(double totalKm, int totalRuns, long totalTimeMs) {
            this.totalKm = totalKm;
            this.totalRuns = totalRuns;
            this.totalTimeMs = totalTimeMs;
        }
    }
}
