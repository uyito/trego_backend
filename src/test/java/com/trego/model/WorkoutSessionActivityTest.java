package com.trego.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WorkoutSessionActivityTest {

    @Test
    void activityFieldsRoundTripThroughFirestoreMap() {
        WorkoutSession s = new WorkoutSession("uid-1", "cardio");
        s.setActivityType("hiking");
        s.setLogKind("distanceCardio");
        s.setDistance(12.5);
        s.setElevationGain(430.0);
        s.setAvgPace(360.0);
        s.setStroke(null);

        Map<String, Object> map = s.toFirestoreMap();
        assertEquals("hiking", map.get("activityType"));
        assertEquals("distanceCardio", map.get("logKind"));
        assertEquals(12.5, map.get("distance"));
        assertEquals(430.0, map.get("elevationGain"));
        assertEquals(360.0, map.get("avgPace"));

        WorkoutSession back = WorkoutSession.fromFirestoreMap(map);
        assertEquals("hiking", back.getActivityType());
        assertEquals("distanceCardio", back.getLogKind());
        assertEquals(12.5, back.getDistance());
        assertEquals(430.0, back.getElevationGain());
        assertEquals(360.0, back.getAvgPace());
    }

    @Test
    void swimmingKeepsStroke() {
        WorkoutSession s = new WorkoutSession("uid-1", "cardio");
        s.setActivityType("swimming");
        s.setLogKind("distanceCardio");
        s.setStroke("freestyle");
        WorkoutSession back = WorkoutSession.fromFirestoreMap(s.toFirestoreMap());
        assertEquals("freestyle", back.getStroke());
    }
}
