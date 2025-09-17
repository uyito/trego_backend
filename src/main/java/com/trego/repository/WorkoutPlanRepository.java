package com.trego.repository;

import com.trego.model.WorkoutPlan;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class WorkoutPlanRepository extends FirestoreRepository<WorkoutPlan> {
    
    public WorkoutPlanRepository() {
        super("workoutPlans", WorkoutPlan::fromFirestoreMap);
    }
    
    public List<WorkoutPlan> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return findByField("userId", userId);
    }
    
    public List<WorkoutPlan> findPublicWorkouts() throws ExecutionException, InterruptedException {
        return findByField("isPublic", true);
    }
    
    public List<WorkoutPlan> findByWorkoutType(String workoutType) throws ExecutionException, InterruptedException {
        return findByField("workoutType", workoutType);
    }
    
    public List<WorkoutPlan> findByDifficulty(String difficulty) throws ExecutionException, InterruptedException {
        return findByField("difficulty", difficulty);
    }
    
    public List<WorkoutPlan> findAiGeneratedWorkouts() throws ExecutionException, InterruptedException {
        return findByField("isAiGenerated", true);
    }
    
    public void deleteAllByUserId(String userId) throws ExecutionException, InterruptedException {
        List<WorkoutPlan> userWorkouts = findByUserId(userId);
        for (WorkoutPlan workout : userWorkouts) {
            deleteById(workout.getId());
        }
    }
}