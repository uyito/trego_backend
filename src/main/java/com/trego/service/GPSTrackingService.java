package com.trego.service;

import com.trego.model.GPSPoint;
import com.trego.model.WorkoutSession;
import com.trego.repository.WorkoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class GPSTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GPSTrackingService.class);
    
    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;
    
    private Map<String, String> activeGPSTrackingSessions = new HashMap<>();
    
    public String startGPSTracking(String userId, String sessionId) throws ExecutionException, InterruptedException {
        logger.info("Starting GPS tracking for user: {} session: {}", userId, sessionId);
        
        Optional<WorkoutSession> sessionOpt = workoutSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Workout session not found or access denied");
        }
        
        activeGPSTrackingSessions.put(userId, sessionId);
        
        WorkoutSession session = sessionOpt.get();
        session.setStatus("GPS_TRACKING");
        workoutSessionRepository.update(session);
        
        logger.info("GPS tracking started for session: {}", sessionId);
        return sessionId;
    }
    
    public void addGPSPoint(String userId, String sessionId, GPSPoint gpsPoint) throws ExecutionException, InterruptedException {
        logger.debug("Adding GPS point for user: {} session: {}", userId, sessionId);
        
        if (!activeGPSTrackingSessions.containsKey(userId) || 
            !activeGPSTrackingSessions.get(userId).equals(sessionId)) {
            throw new IllegalArgumentException("GPS tracking not active for this session");
        }
        
        Optional<WorkoutSession> sessionOpt = workoutSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Workout session not found or access denied");
        }
        
        WorkoutSession session = sessionOpt.get();
        session.addGPSPoint(gpsPoint);
        
        workoutSessionRepository.update(session);
        logger.debug("GPS point added to session: {}", sessionId);
    }
    
    public Map<String, Object> endGPSTracking(String userId, String sessionId) throws ExecutionException, InterruptedException {
        logger.info("Ending GPS tracking for user: {} session: {}", userId, sessionId);
        
        if (!activeGPSTrackingSessions.containsKey(userId)) {
            throw new IllegalArgumentException("No active GPS tracking found for user");
        }
        
        activeGPSTrackingSessions.remove(userId);
        
        Optional<WorkoutSession> sessionOpt = workoutSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Workout session not found or access denied");
        }
        
        WorkoutSession session = sessionOpt.get();
        Map<String, Object> metrics = calculateRouteMetrics(session.getGpsRoute());
        
        session.setStatus("COMPLETED");
        workoutSessionRepository.update(session);
        
        logger.info("GPS tracking ended for session: {} with metrics: {}", sessionId, metrics);
        return metrics;
    }
    
    public Map<String, Object> calculateRouteMetrics(List<GPSPoint> gpsRoute) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (gpsRoute == null || gpsRoute.size() < 2) {
            metrics.put("totalDistance", 0.0);
            metrics.put("averageSpeed", 0.0);
            metrics.put("maxSpeed", 0.0);
            metrics.put("elevationGain", 0.0);
            metrics.put("elevationLoss", 0.0);
            return metrics;
        }
        
        double totalDistance = 0.0;
        double maxSpeed = 0.0;
        double totalElevationGain = 0.0;
        double totalElevationLoss = 0.0;
        List<Double> speeds = new ArrayList<>();
        
        for (int i = 1; i < gpsRoute.size(); i++) {
            GPSPoint current = gpsRoute.get(i);
            GPSPoint previous = gpsRoute.get(i - 1);
            
            // Calculate distance
            double segmentDistance = previous.distanceTo(current);
            totalDistance += segmentDistance;
            
            // Track speeds
            if (current.getSpeed() != null) {
                speeds.add(current.getSpeed());
                maxSpeed = Math.max(maxSpeed, current.getSpeed());
            }
            
            // Calculate elevation changes
            if (current.getAltitude() != null && previous.getAltitude() != null) {
                double elevationChange = current.getAltitude() - previous.getAltitude();
                if (elevationChange > 0) {
                    totalElevationGain += elevationChange;
                } else {
                    totalElevationLoss += Math.abs(elevationChange);
                }
            }
        }
        
        double averageSpeed = speeds.isEmpty() ? 0.0 : speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        metrics.put("totalDistance", Math.round(totalDistance * 100.0) / 100.0);
        metrics.put("averageSpeed", Math.round(averageSpeed * 100.0) / 100.0);
        metrics.put("maxSpeed", Math.round(maxSpeed * 100.0) / 100.0);
        metrics.put("elevationGain", Math.round(totalElevationGain * 100.0) / 100.0);
        metrics.put("elevationLoss", Math.round(totalElevationLoss * 100.0) / 100.0);
        metrics.put("totalPoints", gpsRoute.size());
        
        return metrics;
    }
    
    public Map<String, Object> getGPSTrackingStatus(String userId) {
        Map<String, Object> status = new HashMap<>();
        
        boolean isActive = activeGPSTrackingSessions.containsKey(userId);
        status.put("isActive", isActive);
        
        if (isActive) {
            status.put("sessionId", activeGPSTrackingSessions.get(userId));
            status.put("startedAt", LocalDateTime.now()); // In real implementation, track actual start time
        }
        
        return status;
    }
    
    public List<WorkoutSession> getUserGPSWorkouts(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting GPS workouts for user: {}", userId);
        
        List<WorkoutSession> allSessions = workoutSessionRepository.findByUserId(userId);
        
        return allSessions.stream()
                .filter(session -> session.getGpsRoute() != null && !session.getGpsRoute().isEmpty())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }
}