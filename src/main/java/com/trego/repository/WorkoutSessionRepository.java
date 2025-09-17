package com.trego.repository;

import com.trego.model.WorkoutSession;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class WorkoutSessionRepository extends FirestoreRepository<WorkoutSession> {
    
    public WorkoutSessionRepository() {
        super("workoutSessions", WorkoutSession::fromFirestoreMap);
    }
    
    public List<WorkoutSession> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return findByField("userId", userId);
    }
    
    public List<WorkoutSession> findByUserIdAndType(String userId, String sessionType) throws ExecutionException, InterruptedException {
        List<WorkoutSession> userSessions = findByUserId(userId);
        return userSessions.stream()
                .filter(session -> sessionType.equals(session.getSessionType()))
                .collect(Collectors.toList());
    }
    
    public List<WorkoutSession> findByWorkoutPlanId(String workoutPlanId) throws ExecutionException, InterruptedException {
        return findByField("workoutPlanId", workoutPlanId);
    }
    
    public List<WorkoutSession> findActiveSessionsByUserId(String userId) throws ExecutionException, InterruptedException {
        List<WorkoutSession> userSessions = findByUserId(userId);
        return userSessions.stream()
                .filter(session -> "IN_PROGRESS".equals(session.getStatus()) || "GPS_TRACKING".equals(session.getStatus()))
                .collect(Collectors.toList());
    }
    
    public List<WorkoutSession> findCompletedSessionsByUserId(String userId) throws ExecutionException, InterruptedException {
        List<WorkoutSession> userSessions = findByUserId(userId);
        return userSessions.stream()
                .filter(WorkoutSession::isCompleted)
                .collect(Collectors.toList());
    }
    
    public List<WorkoutSession> findRecentSessions(String userId, int days) throws ExecutionException, InterruptedException {
        List<WorkoutSession> userSessions = findByUserId(userId);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        return userSessions.stream()
                .filter(session -> session.getCreatedAt().isAfter(cutoff))
                .collect(Collectors.toList());
    }
    
    public List<WorkoutSession> findGPSEnabledSessions(String userId) throws ExecutionException, InterruptedException {
        List<WorkoutSession> userSessions = findByUserId(userId);
        return userSessions.stream()
                .filter(session -> session.getGpsRoute() != null && !session.getGpsRoute().isEmpty())
                .collect(Collectors.toList());
    }
    
    public void deleteAllByUserId(String userId) throws ExecutionException, InterruptedException {
        List<WorkoutSession> userSessions = findByUserId(userId);
        for (WorkoutSession session : userSessions) {
            deleteById(session.getId());
        }
    }
}