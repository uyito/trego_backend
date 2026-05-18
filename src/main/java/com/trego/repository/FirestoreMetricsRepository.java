// trego_backend/src/main/java/com/trego/repository/FirestoreMetricsRepository.java
package com.trego.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.trego.dto.*;
import com.trego.model.RunRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreMetricsRepository implements MetricsRepository {

    @Autowired
    private Firestore firestore;

    @Override
    public List<RunRecord> readRuns(String uid) {
        try {
            QuerySnapshot snap = firestore
                    .collection("users").document(uid)
                    .collection("runs")
                    .orderBy("startTime", Query.Direction.ASCENDING)
                    .get().get();
            List<RunRecord> out = new ArrayList<>(snap.size());
            for (DocumentSnapshot d : snap.getDocuments()) {
                out.add(new RunRecord(
                        d.getId(),
                        ((Number) d.get("distance")).doubleValue(),
                        ((Number) d.get("duration")).longValue(),
                        ((Timestamp) d.get("startTime")).toSqlTimestamp().toInstant()
                ));
            }
            return out;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("readRuns failed for " + uid, e);
        }
    }

    @Override
    public int countRuns(String uid) {
        try {
            AggregateQuerySnapshot agg = firestore
                    .collection("users").document(uid)
                    .collection("runs")
                    .count().get().get();
            return (int) agg.getCount();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("countRuns failed for " + uid, e);
        }
    }

    @Override
    public Optional<StoredSnapshot> readSnapshot(String uid) {
        try {
            DocumentSnapshot d = firestore
                    .collection("users").document(uid)
                    .collection("metrics").document("snapshot")
                    .get().get();
            if (!d.exists()) return Optional.empty();
            int schemaVersion = ((Number) d.get("schemaVersion")).intValue();
            int runCountAtCompute = ((Number) d.get("runCountAtCompute")).intValue();
            MetricsSnapshotDto dto = parseSnapshotDto(d);
            return Optional.of(new StoredSnapshot(schemaVersion, runCountAtCompute, dto));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("readSnapshot failed for " + uid, e);
        }
    }

    @Override
    public void writeSnapshot(String uid, StoredSnapshot snapshot, Map<LocalDate, DailyAggregate> daily) {
        try {
            CollectionReference metrics = firestore.collection("users").document(uid).collection("metrics");

            // Snapshot doc
            Map<String, Object> snapMap = serializeSnapshot(snapshot);
            metrics.document("snapshot").set(snapMap).get();

            // Daily docs — batch in groups of 500 (Firestore limit).
            // NOTE: spec describes daily storage as users/{uid}/metrics/daily/{yyyy-mm-dd},
            // but Firestore paths must alternate collection/doc. We therefore store daily
            // docs as siblings to the `snapshot` doc within the `metrics` collection,
            // with doc IDs of the form `daily-{yyyy-mm-dd}`. Same data shape; only path differs.
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-mm-dd
            WriteBatch batch = firestore.batch();
            int counter = 0;
            for (Map.Entry<LocalDate, DailyAggregate> e : daily.entrySet()) {
                String docId = "daily-" + e.getKey().format(fmt);
                Map<String, Object> dayMap = new HashMap<>();
                dayMap.put("date", e.getKey().format(fmt));
                dayMap.put("totalKm", e.getValue().totalKm);
                dayMap.put("totalRuns", e.getValue().totalRuns);
                dayMap.put("totalTimeMs", e.getValue().totalTimeMs);
                batch.set(metrics.document(docId), dayMap);
                counter++;
                if (counter % 500 == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }
            if (counter % 500 != 0) batch.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("writeSnapshot failed for " + uid, e);
        }
    }

    // --- (de)serialization helpers ---

    private Map<String, Object> serializeSnapshot(StoredSnapshot s) {
        Map<String, Object> m = new HashMap<>();
        m.put("schemaVersion", s.schemaVersion);
        m.put("runCountAtCompute", s.runCountAtCompute);
        m.put("computedAt", Timestamp.ofTimeSecondsAndNanos(
                s.dto.getComputedAt().getEpochSecond(), s.dto.getComputedAt().getNano()));
        m.put("thisWeek", serializeWeek(s.dto.getThisWeek()));
        m.put("prs", serializePrs(s.dto.getPrs()));
        m.put("totals", serializeTotals(s.dto.getTotals()));
        List<Map<String, Object>> hist = new ArrayList<>();
        for (WeeklyMetricsDto w : s.dto.getHistory()) hist.add(serializeWeek(w));
        m.put("history", hist);
        return m;
    }

    private Map<String, Object> serializeWeek(WeeklyMetricsDto w) {
        Map<String, Object> m = new HashMap<>();
        m.put("isoYearWeek", w.getIsoYearWeek());
        m.put("weekStart", instantToTs(w.getWeekStart()));
        m.put("weekEnd", instantToTs(w.getWeekEnd()));
        m.put("totalKm", w.getTotalKm());
        m.put("totalRuns", w.getTotalRuns());
        m.put("totalTimeMs", w.getTotalTimeMs());
        m.put("avgPaceSecPerKm", w.getAvgPaceSecPerKm());
        m.put("longestKm", w.getLongestKm());
        m.put("streakDays", w.getStreakDays());
        return m;
    }

    private Map<String, Object> serializePrs(PrsDto p) {
        Map<String, Object> m = new HashMap<>();
        m.put("fastest1k", p.getFastest1k() == null ? null : serializePr(p.getFastest1k()));
        m.put("fastest5k", p.getFastest5k() == null ? null : serializePr(p.getFastest5k()));
        m.put("fastest10k", p.getFastest10k() == null ? null : serializePr(p.getFastest10k()));
        m.put("longestDistance", p.getLongestDistance() == null ? null : serializePr(p.getLongestDistance()));
        m.put("longestDuration", p.getLongestDuration() == null ? null : serializePr(p.getLongestDuration()));
        return m;
    }

    private Map<String, Object> serializePr(PrEntryDto p) {
        Map<String, Object> m = new HashMap<>();
        m.put("runId", p.getRunId());
        m.put("distanceKm", p.getDistanceKm());
        m.put("runStartTime", instantToTs(p.getRunStartTime()));
        if (p.getPaceSecPerKm() != null) m.put("paceSecPerKm", p.getPaceSecPerKm());
        if (p.getDurationMs() != null) m.put("durationMs", p.getDurationMs());
        return m;
    }

    private Map<String, Object> serializeTotals(LifetimeTotalsDto t) {
        Map<String, Object> m = new HashMap<>();
        m.put("totalKm", t.getTotalKm());
        m.put("totalRuns", t.getTotalRuns());
        m.put("totalTimeMs", t.getTotalTimeMs());
        return m;
    }

    private Timestamp instantToTs(Instant i) {
        return i == null ? null : Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano());
    }

    @SuppressWarnings("unchecked")
    private MetricsSnapshotDto parseSnapshotDto(DocumentSnapshot d) {
        MetricsSnapshotDto dto = new MetricsSnapshotDto();
        dto.setComputedAt(((Timestamp) d.get("computedAt")).toSqlTimestamp().toInstant());
        dto.setThisWeek(parseWeek((Map<String, Object>) d.get("thisWeek")));
        dto.setPrs(parsePrs((Map<String, Object>) d.get("prs")));
        dto.setTotals(parseTotals((Map<String, Object>) d.get("totals")));
        List<Map<String, Object>> hist = (List<Map<String, Object>>) d.get("history");
        List<WeeklyMetricsDto> weeks = new ArrayList<>();
        for (Map<String, Object> w : hist) weeks.add(parseWeek(w));
        dto.setHistory(weeks);
        return dto;
    }

    private WeeklyMetricsDto parseWeek(Map<String, Object> m) {
        WeeklyMetricsDto w = new WeeklyMetricsDto();
        w.setIsoYearWeek((String) m.get("isoYearWeek"));
        w.setWeekStart(((Timestamp) m.get("weekStart")).toSqlTimestamp().toInstant());
        w.setWeekEnd(((Timestamp) m.get("weekEnd")).toSqlTimestamp().toInstant());
        w.setTotalKm(((Number) m.get("totalKm")).doubleValue());
        w.setTotalRuns(((Number) m.get("totalRuns")).intValue());
        w.setTotalTimeMs(((Number) m.get("totalTimeMs")).longValue());
        w.setAvgPaceSecPerKm(((Number) m.get("avgPaceSecPerKm")).longValue());
        w.setLongestKm(((Number) m.get("longestKm")).doubleValue());
        w.setStreakDays(((Number) m.get("streakDays")).intValue());
        return w;
    }

    @SuppressWarnings("unchecked")
    private PrsDto parsePrs(Map<String, Object> m) {
        PrsDto p = new PrsDto();
        p.setFastest1k(parsePr((Map<String, Object>) m.get("fastest1k")));
        p.setFastest5k(parsePr((Map<String, Object>) m.get("fastest5k")));
        p.setFastest10k(parsePr((Map<String, Object>) m.get("fastest10k")));
        p.setLongestDistance(parsePr((Map<String, Object>) m.get("longestDistance")));
        p.setLongestDuration(parsePr((Map<String, Object>) m.get("longestDuration")));
        return p;
    }

    private PrEntryDto parsePr(Map<String, Object> m) {
        if (m == null) return null;
        PrEntryDto p = new PrEntryDto();
        p.setRunId((String) m.get("runId"));
        p.setDistanceKm(((Number) m.get("distanceKm")).doubleValue());
        p.setRunStartTime(((Timestamp) m.get("runStartTime")).toSqlTimestamp().toInstant());
        if (m.get("paceSecPerKm") != null) p.setPaceSecPerKm(((Number) m.get("paceSecPerKm")).longValue());
        if (m.get("durationMs") != null) p.setDurationMs(((Number) m.get("durationMs")).longValue());
        return p;
    }

    private LifetimeTotalsDto parseTotals(Map<String, Object> m) {
        LifetimeTotalsDto t = new LifetimeTotalsDto();
        t.setTotalKm(((Number) m.get("totalKm")).doubleValue());
        t.setTotalRuns(((Number) m.get("totalRuns")).intValue());
        t.setTotalTimeMs(((Number) m.get("totalTimeMs")).longValue());
        return t;
    }
}
