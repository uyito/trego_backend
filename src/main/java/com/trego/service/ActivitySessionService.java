package com.trego.service;

import com.trego.model.ExerciseLog;
import com.trego.model.ExerciseSet;
import com.trego.model.WorkoutSession;
import com.trego.repository.WorkoutSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** Persists and reads manually-logged multi-activity sessions. */
@Service
public class ActivitySessionService {

    private final WorkoutSessionRepository repo;

    @Autowired
    public ActivitySessionService(WorkoutSessionRepository repo) {
        this.repo = repo;
    }

    public WorkoutSession logSession(String uid, Map<String, Object> p) throws Exception {
        String logKind = str(p.get("logKind"));
        WorkoutSession s = new WorkoutSession(uid, categoryFor(logKind));
        s.setActivityType(str(p.get("activityType")));
        s.setLogKind(logKind);
        s.setSessionName(str(p.get("sessionName")));
        s.setNotes(str(p.get("notes")));
        s.setDuration(intOrNull(p.get("duration")));
        s.setDistance(dblOrNull(p.get("distance")));
        s.setElevationGain(dblOrNull(p.get("elevationGain")));
        s.setStroke(str(p.get("stroke")));
        s.setPerceivedExertion(intOrNull(p.get("perceivedExertion")));
        if (s.getDistance() != null && s.getDuration() != null && s.getDistance() > 0) {
            s.setAvgPace((s.getDuration() * 60.0) / s.getDistance()); // sec per km
        }
        s.setExercises(parseExercises(p.get("exercises")));
        s.setCompleted(true);
        s.setStatus("COMPLETED");
        repo.save(s);
        return s;
    }

    public List<WorkoutSession> getHistory(String uid) throws Exception {
        return repo.findByUserId(uid).stream()
                .sorted(Comparator.comparing(WorkoutSession::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public WorkoutSession getSession(String uid, String id) throws Exception {
        return repo.findByUserId(uid).stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<ExerciseLog> parseExercises(Object raw) {
        List<ExerciseLog> out = new ArrayList<>();
        if (!(raw instanceof List)) return out;
        for (Object eo : (List<Object>) raw) {
            if (!(eo instanceof Map)) continue;
            Map<String, Object> em = (Map<String, Object>) eo;
            ExerciseLog log = new ExerciseLog(str(em.get("exerciseId")), str(em.get("name")));
            List<ExerciseSet> sets = new ArrayList<>();
            Object rawSets = em.get("sets");
            if (rawSets instanceof List) {
                int n = 1;
                for (Object so : (List<Object>) rawSets) {
                    if (!(so instanceof Map)) continue;
                    Map<String, Object> sm = (Map<String, Object>) so;
                    ExerciseSet set = new ExerciseSet();
                    set.setSetNumber(intOrNull(sm.getOrDefault("setNumber", n)));
                    set.setReps(intOrNull(sm.get("reps")));
                    set.setWeight(dblOrNull(sm.get("weight")));
                    set.setRpe(intOrNull(sm.get("rpe")));
                    set.setRestTime(intOrNull(sm.get("restTime")));
                    sets.add(set);
                    n++;
                }
            }
            log.setSets(sets);
            out.add(log);
        }
        return out;
    }

    private static String categoryFor(String logKind) {
        return logKind != null ? logKind : "duration";
    }
    private static String str(Object o) { return o != null ? o.toString() : null; }
    private static Integer intOrNull(Object o) { return o instanceof Number ? ((Number) o).intValue() : null; }
    private static Double dblOrNull(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : null; }
}
