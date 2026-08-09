package com.trego.service;

import com.trego.model.WorkoutSession;
import com.trego.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivitySessionServiceTest {

    WorkoutSessionRepository repo;
    ActivitySessionService service;

    @BeforeEach
    void setUp() {
        repo = mock(WorkoutSessionRepository.class);
        service = new ActivitySessionService(repo);
    }

    @Test
    void logSessionPersistsWithUserAndFields() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("activityType", "hiking");
        payload.put("logKind", "distanceCardio");
        payload.put("sessionName", "Morning hike");
        payload.put("duration", 95);
        payload.put("distance", 12.5);
        payload.put("elevationGain", 430.0);

        WorkoutSession saved = service.logSession("uid-1", payload);

        assertEquals("uid-1", saved.getUserId());
        assertEquals("hiking", saved.getActivityType());
        assertEquals("distanceCardio", saved.getLogKind());
        assertEquals(12.5, saved.getDistance());
        assertTrue(saved.isCompleted());
        verify(repo).save(any(WorkoutSession.class));
    }

    @Test
    void getHistoryReturnsNewestFirst() throws Exception {
        WorkoutSession older = new WorkoutSession("uid-1", "cardio");
        older.setId("a");
        older.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 8, 0));
        WorkoutSession newer = new WorkoutSession("uid-1", "strength");
        newer.setId("b");
        newer.setCreatedAt(java.time.LocalDateTime.of(2026, 2, 1, 8, 0));
        when(repo.findByUserId("uid-1")).thenReturn(new ArrayList<>(List.of(older, newer)));

        List<WorkoutSession> out = service.getHistory("uid-1");
        assertEquals("b", out.get(0).getId());
        assertEquals("a", out.get(1).getId());
    }

    @Test
    void getSessionReturnsNullWhenNotOwned() throws Exception {
        WorkoutSession s = new WorkoutSession("other-uid", "cardio");
        s.setId("x");
        when(repo.findByUserId("uid-1")).thenReturn(new ArrayList<>());
        assertNull(service.getSession("uid-1", "x"));
    }
}
