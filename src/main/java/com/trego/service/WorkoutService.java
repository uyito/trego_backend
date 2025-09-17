package com.trego.service;

import com.trego.model.User;
import com.trego.model.UserProfile;
import com.trego.model.WorkoutPlan;
import com.trego.repository.UserProfileRepository;
import com.trego.repository.UserRepository;
import com.trego.repository.WorkoutPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class WorkoutService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkoutService.class);
    
    @Autowired
    private WorkoutPlanRepository workoutPlanRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OpenAIService openAIService;
    
    public WorkoutPlan createWorkoutPlan(String userId, WorkoutPlan workoutPlan) throws ExecutionException, InterruptedException {
        logger.info("Creating workout plan for user: {}", userId);
        
        workoutPlan.setUserId(userId);
        
        // Estimate calories if not provided
        if (workoutPlan.getCaloriesBurnedEstimate() == null) {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                double estimate = estimateCalories(profileOpt.get(), workoutPlan);
                workoutPlan.setCaloriesBurnedEstimate(estimate);
            }
        }
        
        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);
        logger.info("Workout plan created with ID: {}", savedPlan.getId());
        
        return savedPlan;
    }
    
    public WorkoutPlan generateAIWorkoutPlan(String userId) throws ExecutionException, InterruptedException {
        logger.info("Generating AI workout plan for user: {}", userId);
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User profile not found. Please complete your profile first.");
        }
        
        UserProfile profile = profileOpt.get();
        
        // Check if user has premium access for AI features
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        if (!user.isPremiumUser() && !user.isTrialActive()) {
            throw new IllegalArgumentException("AI workout generation requires premium subscription");
        }
        
        WorkoutPlan aiPlan = openAIService.generatePersonalizedWorkout(profile);
        aiPlan.setUserId(userId);
        
        WorkoutPlan savedPlan = workoutPlanRepository.save(aiPlan);
        logger.info("AI workout plan generated and saved for user: {}", userId);
        
        return savedPlan;
    }
    
    public List<WorkoutPlan> getUserWorkoutPlans(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting workout plans for user: {}", userId);
        
        List<WorkoutPlan> workoutPlans = workoutPlanRepository.findByUserId(userId);
        
        // Sort by creation date, newest first
        workoutPlans.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        logger.info("Found {} workout plans for user: {}", workoutPlans.size(), userId);
        return workoutPlans;
    }
    
    public List<WorkoutPlan> getPublicWorkoutPlans(int limit) throws ExecutionException, InterruptedException {
        logger.info("Getting public workout plans with limit: {}", limit);
        
        List<WorkoutPlan> publicPlans = workoutPlanRepository.findPublicWorkouts();
        
        // Sort by rating and completion count
        return publicPlans.stream()
                .sorted((a, b) -> {
                    int ratingCompare = Double.compare(b.getAverageRating(), a.getAverageRating());
                    if (ratingCompare != 0) return ratingCompare;
                    return Integer.compare(b.getCompletedCount(), a.getCompletedCount());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<WorkoutPlan> getRecommendedWorkouts(String userId, int limit) throws ExecutionException, InterruptedException {
        logger.info("Getting recommended workouts for user: {}", userId);
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return getPublicWorkoutPlans(limit);
        }
        
        UserProfile profile = profileOpt.get();
        List<WorkoutPlan> allPublicPlans = workoutPlanRepository.findPublicWorkouts();
        
        // Filter and score workouts based on user preferences
        return allPublicPlans.stream()
                .filter(plan -> isWorkoutSuitable(plan, profile))
                .sorted((a, b) -> Double.compare(calculateWorkoutScore(b, profile), calculateWorkoutScore(a, profile)))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public WorkoutPlan completeWorkout(String userId, String workoutPlanId, Map<String, Object> completionData) 
            throws ExecutionException, InterruptedException {
        
        logger.info("Marking workout as completed - User: {}, Workout: {}", userId, workoutPlanId);
        
        Optional<WorkoutPlan> planOpt = workoutPlanRepository.findById(workoutPlanId);
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Workout plan not found");
        }
        
        WorkoutPlan plan = planOpt.get();
        plan.incrementCompletedCount();
        workoutPlanRepository.update(plan);
        
        // Update user profile stats
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            updateWorkoutStats(profile, plan, completionData);
            userProfileRepository.update(profile);
        }
        
        logger.info("Workout completion recorded successfully");
        return plan;
    }
    
    public WorkoutPlan rateWorkout(String workoutPlanId, Double rating) throws ExecutionException, InterruptedException {
        logger.info("Rating workout plan: {} with rating: {}", workoutPlanId, rating);
        
        Optional<WorkoutPlan> planOpt = workoutPlanRepository.findById(workoutPlanId);
        if (planOpt.isEmpty()) {
            throw new IllegalArgumentException("Workout plan not found");
        }
        
        WorkoutPlan plan = planOpt.get();
        plan.updateRating(rating);
        
        WorkoutPlan updatedPlan = workoutPlanRepository.update(plan);
        logger.info("Workout rated successfully");
        
        return updatedPlan;
    }
    
    private boolean isWorkoutSuitable(WorkoutPlan plan, UserProfile profile) {
        // Check difficulty level
        if (profile.getExperience() != null && plan.getDifficulty() != null) {
            if ("beginner".equals(profile.getExperience()) && "advanced".equals(plan.getDifficulty())) {
                return false;
            }
        }
        
        // Check duration preference
        if (profile.getWorkoutDuration() != null && plan.getDuration() != null) {
            int durationDiff = Math.abs(profile.getWorkoutDuration() - plan.getDuration());
            if (durationDiff > 30) { // More than 30 minutes difference
                return false;
            }
        }
        
        return true;
    }
    
    private double calculateWorkoutScore(WorkoutPlan plan, UserProfile profile) {
        double score = 0.0;
        
        // Base score from rating and popularity
        score += plan.getAverageRating() * 20; // 0-100 points
        score += Math.min(plan.getCompletedCount() / 10.0, 20); // Up to 20 points for popularity
        
        // Bonus for matching user preferences
        if (profile.getExperience() != null && profile.getExperience().equals(plan.getDifficulty())) {
            score += 15;
        }
        
        if (profile.getWorkoutDuration() != null && plan.getDuration() != null) {
            int durationDiff = Math.abs(profile.getWorkoutDuration() - plan.getDuration());
            score += Math.max(0, 10 - (durationDiff / 10.0)); // Up to 10 points for duration match
        }
        
        // Bonus for matching fitness goals
        if (profile.getFitnessGoals() != null && plan.getTags() != null) {
            long matchingGoals = profile.getFitnessGoals().stream()
                    .mapToLong(goal -> plan.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(goal.toLowerCase())) ? 1 : 0)
                    .sum();
            score += matchingGoals * 5;
        }
        
        return score;
    }
    
    private void updateWorkoutStats(UserProfile profile, WorkoutPlan plan, Map<String, Object> completionData) {
        Map<String, Object> stats = profile.getStats();
        
        // Update total workouts
        int totalWorkouts = (Integer) stats.getOrDefault("totalWorkouts", 0);
        stats.put("totalWorkouts", totalWorkouts + 1);
        
        // Update calories burned
        double totalCalories = (Double) stats.getOrDefault("totalCaloriesBurned", 0.0);
        double workoutCalories = plan.getCaloriesBurnedEstimate() != null ? plan.getCaloriesBurnedEstimate() : 200.0;
        stats.put("totalCaloriesBurned", totalCalories + workoutCalories);
        
        // Update average workout duration
        int avgDuration = (Integer) stats.getOrDefault("averageWorkoutDuration", 0);
        int newDuration = plan.getDuration() != null ? plan.getDuration() : 60;
        stats.put("averageWorkoutDuration", (avgDuration * totalWorkouts + newDuration) / (totalWorkouts + 1));
        
        // Update current streak
        updateWorkoutStreak(stats);
        
        profile.setStats(stats);
        profile.updateTimestamp();
    }
    
    private void updateWorkoutStreak(Map<String, Object> stats) {
        int currentStreak = (Integer) stats.getOrDefault("currentStreak", 0);
        int longestStreak = (Integer) stats.getOrDefault("longestStreak", 0);
        
        currentStreak++;
        stats.put("currentStreak", currentStreak);
        
        if (currentStreak > longestStreak) {
            stats.put("longestStreak", currentStreak);
        }
    }
    
    private double estimateCalories(UserProfile profile, WorkoutPlan plan) {
        double baseCalories = 200.0;
        
        if (profile.getWeight() != null && plan.getDuration() != null) {
            double weight = profile.getWeight();
            double hours = plan.getDuration() / 60.0;
            
            double mets = switch (plan.getWorkoutType() != null ? plan.getWorkoutType() : "general") {
                case "hiit" -> 8.0;
                case "strength" -> 6.0;
                case "cardio" -> 7.0;
                case "yoga" -> 3.0;
                default -> 5.0;
            };
            
            baseCalories = mets * weight * hours;
        }
        
        return Math.round(baseCalories);
    }
}