package com.trego.service;

import com.trego.model.*;
import com.trego.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivitySessionPrsTest {

    WorkoutSessionRepository repo;
    ActivitySessionService service;

    @BeforeEach void setUp() {
        repo = mock(WorkoutSessionRepository.class);
        service = new ActivitySessionService(repo);
    }

    @Test
    void cardioPrsPickBestDistanceAndElevation() throws Exception {
        WorkoutSession a = new WorkoutSession("u", "cardio");
        a.setActivityType("running"); a.setLogKind("distanceCardio");
        a.setDistance(5.0); a.setElevationGain(50.0); a.setAvgPace(300.0);
        WorkoutSession b = new WorkoutSession("u", "cardio");
        b.setActivityType("running"); b.setLogKind("distanceCardio");
        b.setDistance(10.0); b.setElevationGain(120.0); b.setAvgPace(330.0);
        when(repo.findByUserId("u")).thenReturn(new ArrayList<>(List.of(a, b)));

        Map<String, Object> pr = findByActivity(service.computePRs("u"), "running");
        assertEquals(10.0, pr.get("bestDistance"));
        assertEquals(120.0, pr.get("bestElevation"));
        assertEquals(300.0, pr.get("bestPace")); // lower = better
    }

    @Test
    void strengthPrsComputeHeaviestAndEpley() throws Exception {
        WorkoutSession s = new WorkoutSession("u", "strength");
        s.setActivityType("bench-press"); s.setLogKind("strength");
        ExerciseLog log = new ExerciseLog("bench-press", "Bench Press");
        ExerciseSet set1 = new ExerciseSet(); set1.setReps(5); set1.setWeight(80.0);
        ExerciseSet set2 = new ExerciseSet(); set2.setReps(10); set2.setWeight(70.0);
        log.setSets(new ArrayList<>(List.of(set1, set2)));
        s.setExercises(new ArrayList<>(List.of(log)));
        when(repo.findByUserId("u")).thenReturn(new ArrayList<>(List.of(s)));

        Map<String, Object> pr = findByExercise(service.computePRs("u"), "bench-press");
        assertEquals(80.0, pr.get("heaviestWeight"));
        // Epley best of: 80*(1+5/30)=93.33 vs 70*(1+10/30)=93.33 -> ~93.33
        assertEquals(93.33, (double) pr.get("estimatedOneRepMax"), 0.1);
    }

    private static Map<String,Object> findByActivity(List<Map<String,Object>> prs, String at) {
        return prs.stream().filter(m -> at.equals(m.get("activityType"))).findFirst().orElseThrow();
    }
    private static Map<String,Object> findByExercise(List<Map<String,Object>> prs, String id) {
        return prs.stream().filter(m -> id.equals(m.get("exerciseId"))).findFirst().orElseThrow();
    }
}
