package com.trego.service;

import com.trego.repository.MetricsRepository;
import com.trego.dto.WeeklyGoalDto;
import com.trego.model.RunRecord;

import java.time.LocalDate;
import java.util.*;

public class InMemoryMetricsRepository implements MetricsRepository {
    public final Map<String, List<RunRecord>> runsByUser = new HashMap<>();
    public final Map<String, StoredSnapshot> snapshotsByUser = new HashMap<>();
    public final Map<String, Map<LocalDate, DailyAggregate>> dailyByUser = new HashMap<>();
    public final Map<String, WeeklyGoalDto> goalsByUser = new HashMap<>();

    @Override
    public List<RunRecord> readRuns(String uid) {
        return runsByUser.getOrDefault(uid, List.of());
    }

    @Override
    public int countRuns(String uid) {
        return readRuns(uid).size();
    }

    @Override
    public Optional<StoredSnapshot> readSnapshot(String uid) {
        return Optional.ofNullable(snapshotsByUser.get(uid));
    }

    @Override
    public void writeSnapshot(String uid, StoredSnapshot snapshot, Map<LocalDate, DailyAggregate> daily) {
        snapshotsByUser.put(uid, snapshot);
        dailyByUser.put(uid, new HashMap<>(daily));
    }

    @Override
    public Optional<WeeklyGoalDto> readGoal(String uid) {
        return Optional.ofNullable(goalsByUser.get(uid));
    }

    @Override
    public void writeGoal(String uid, WeeklyGoalDto goal) {
        goalsByUser.put(uid, goal);
    }
}
