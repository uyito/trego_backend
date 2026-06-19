package com.trego.service;

import com.trego.repository.MetricsRepository;
import com.trego.model.RunRecord;

import java.time.LocalDate;
import java.util.*;

public class InMemoryMetricsRepository implements MetricsRepository {
    public final Map<String, List<RunRecord>> runsByUser = new HashMap<>();
    public final Map<String, StoredSnapshot> snapshotsByUser = new HashMap<>();
    public final Map<String, Map<LocalDate, DailyAggregate>> dailyByUser = new HashMap<>();

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
}
