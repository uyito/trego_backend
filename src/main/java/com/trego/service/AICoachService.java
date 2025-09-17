package com.trego.service;

import com.trego.model.User;
import com.trego.model.UserProfile;
import com.trego.model.WorkoutSession;
import com.trego.model.NutritionEntry;
import com.trego.repository.UserProfileRepository;
import com.trego.repository.UserRepository;
import com.trego.repository.WorkoutSessionRepository;
import com.trego.repository.NutritionEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class AICoachService {
    
    private static final Logger logger = LoggerFactory.getLogger(AICoachService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;
    
    @Autowired
    private NutritionEntryRepository nutritionEntryRepository;
    
    @Autowired
    private OpenAIService openAIService;
    
    public Map<String, Object> getPersonalizedRecommendations(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting personalized recommendations for user: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        if (!user.isPremiumUser() && !user.isTrialActive()) {
            throw new IllegalArgumentException("AI Coach requires premium subscription");
        }
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User profile not found");
        }
        
        UserProfile profile = profileOpt.get();
        Map<String, Object> analysisData = getProgressAnalysis(userId);
        
        Map<String, Object> recommendations = new HashMap<>();
        
        // Workout recommendations
        List<String> workoutRecommendations = generateWorkoutRecommendations(profile, analysisData);
        recommendations.put("workoutRecommendations", workoutRecommendations);
        
        // Nutrition recommendations
        List<String> nutritionRecommendations = generateNutritionRecommendations(profile, analysisData);
        recommendations.put("nutritionRecommendations", nutritionRecommendations);
        
        // Motivational message
        String motivationalMessage = generateMotivationalMessage(profile, analysisData);
        recommendations.put("motivationalMessage", motivationalMessage);
        
        // Goal adjustments
        Map<String, Object> goalAdjustments = suggestGoalAdjustments(profile, analysisData);
        recommendations.put("goalAdjustments", goalAdjustments);
        
        logger.info("Generated personalized recommendations for user: {}", userId);
        return recommendations;
    }
    
    public Map<String, Object> getProgressAnalysis(String userId) throws ExecutionException, InterruptedException {
        logger.info("Analyzing progress for user: {}", userId);
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Get recent workout data (last 30 days)
        List<WorkoutSession> recentSessions = getRecentWorkoutSessions(userId, 30);
        analysis.put("workoutAnalysis", analyzeWorkoutProgress(recentSessions));
        
        // Get recent nutrition data (last 30 days)
        List<NutritionEntry> recentNutrition = getRecentNutritionEntries(userId, 30);
        analysis.put("nutritionAnalysis", analyzeNutritionProgress(recentNutrition));
        
        // Calculate trends and patterns
        analysis.put("trends", calculateTrends(recentSessions, recentNutrition));
        
        // Goal progress
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            analysis.put("goalProgress", analyzeGoalProgress(profileOpt.get(), recentSessions, recentNutrition));
        }
        
        return analysis;
    }
    
    public String generateMotivationalMessage(UserProfile profile, Map<String, Object> analysisData) {
        logger.info("Generating motivational message for user profile");
        
        try {
            return openAIService.generateMotivationalMessage(profile, analysisData);
        } catch (Exception e) {
            logger.warn("Failed to generate AI motivational message, using fallback: {}", e.getMessage());
            return generateFallbackMotivationalMessage(profile, analysisData);
        }
    }
    
    public String chatWithAICoach(String userId, String message) throws ExecutionException, InterruptedException {
        logger.info("AI Coach chat for user: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        if (!user.isPremiumUser() && !user.isTrialActive()) {
            throw new IllegalArgumentException("AI Coach chat requires premium subscription");
        }
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User profile not found");
        }
        
        UserProfile profile = profileOpt.get();
        Map<String, Object> context = getProgressAnalysis(userId);
        
        return openAIService.generateCoachResponse(profile, message, context);
    }
    
    private List<String> generateWorkoutRecommendations(UserProfile profile, Map<String, Object> analysisData) {
        List<String> recommendations = new ArrayList<>();
        
        Map<String, Object> workoutAnalysis = (Map<String, Object>) analysisData.get("workoutAnalysis");
        if (workoutAnalysis != null) {
            Integer weeklyWorkouts = (Integer) workoutAnalysis.get("weeklyAverage");
            
            if (weeklyWorkouts != null && weeklyWorkouts < 3) {
                recommendations.add("Try to increase your workout frequency to 3-4 times per week for optimal results");
            }
            
            Double avgDuration = (Double) workoutAnalysis.get("averageDuration");
            if (avgDuration != null && avgDuration < 30) {
                recommendations.add("Consider extending your workouts to 30-45 minutes for better fitness gains");
            }
        }
        
        // Add recommendations based on fitness goals
        if (profile.getFitnessGoals() != null) {
            if (profile.getFitnessGoals().contains("weight_loss")) {
                recommendations.add("Focus on high-intensity interval training (HIIT) to maximize calorie burn");
            }
            if (profile.getFitnessGoals().contains("muscle_gain")) {
                recommendations.add("Incorporate more strength training with progressive overload");
            }
        }
        
        return recommendations;
    }
    
    private List<String> generateNutritionRecommendations(UserProfile profile, Map<String, Object> analysisData) {
        List<String> recommendations = new ArrayList<>();
        
        Map<String, Object> nutritionAnalysis = (Map<String, Object>) analysisData.get("nutritionAnalysis");
        if (nutritionAnalysis != null) {
            Double avgCalories = (Double) nutritionAnalysis.get("averageCalories");
            Double targetCalories = profile.getTDEE();
            
            if (avgCalories != null && targetCalories != null) {
                if (avgCalories < targetCalories * 0.8) {
                    recommendations.add("You're eating below your target calories. Consider adding healthy snacks");
                } else if (avgCalories > targetCalories * 1.2) {
                    recommendations.add("You're exceeding your calorie target. Focus on portion control");
                }
            }
            
            Double avgProtein = (Double) nutritionAnalysis.get("averageProtein");
            if (avgProtein != null && avgProtein < 1.2 * profile.getCurrentWeight()) {
                recommendations.add("Increase your protein intake to support your fitness goals");
            }
        }
        
        return recommendations;
    }
    
    private String generateFallbackMotivationalMessage(UserProfile profile, Map<String, Object> analysisData) {
        List<String> messages = Arrays.asList(
            "Keep pushing forward! Every workout brings you closer to your goals.",
            "Consistency is key! You're building great habits.",
            "Your dedication is inspiring. Stay strong!",
            "Progress isn't always visible, but it's happening. Keep going!",
            "You're stronger than you think. Keep up the amazing work!"
        );
        
        return messages.get(new Random().nextInt(messages.size()));
    }
    
    private Map<String, Object> analyzeWorkoutProgress(List<WorkoutSession> sessions) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (sessions.isEmpty()) {
            analysis.put("weeklyAverage", 0);
            analysis.put("averageDuration", 0.0);
            analysis.put("totalCaloriesBurned", 0.0);
            return analysis;
        }
        
        int totalSessions = sessions.size();
        double avgDuration = sessions.stream()
                .filter(s -> s.getDuration() != null)
                .mapToInt(WorkoutSession::getDuration)
                .average()
                .orElse(0.0);
        
        double totalCalories = sessions.stream()
                .filter(s -> s.getTotalCaloriesBurned() != null)
                .mapToDouble(WorkoutSession::getTotalCaloriesBurned)
                .sum();
        
        analysis.put("totalSessions", totalSessions);
        analysis.put("weeklyAverage", totalSessions / 4); // Assuming 30 days = ~4 weeks
        analysis.put("averageDuration", Math.round(avgDuration));
        analysis.put("totalCaloriesBurned", Math.round(totalCalories));
        
        return analysis;
    }
    
    private Map<String, Object> analyzeNutritionProgress(List<NutritionEntry> entries) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (entries.isEmpty()) {
            analysis.put("averageCalories", 0.0);
            analysis.put("averageProtein", 0.0);
            analysis.put("averageCarbs", 0.0);
            analysis.put("averageFat", 0.0);
            return analysis;
        }
        
        double avgCalories = entries.stream()
                .mapToDouble(NutritionEntry::getCalories)
                .average()
                .orElse(0.0);
        
        double avgProtein = entries.stream()
                .mapToDouble(NutritionEntry::getProtein)
                .average()
                .orElse(0.0);
        
        double avgCarbs = entries.stream()
                .mapToDouble(NutritionEntry::getCarbs)
                .average()
                .orElse(0.0);
        
        double avgFat = entries.stream()
                .mapToDouble(NutritionEntry::getFat)
                .average()
                .orElse(0.0);
        
        analysis.put("entriesCount", entries.size());
        analysis.put("averageCalories", Math.round(avgCalories));
        analysis.put("averageProtein", Math.round(avgProtein * 100.0) / 100.0);
        analysis.put("averageCarbs", Math.round(avgCarbs * 100.0) / 100.0);
        analysis.put("averageFat", Math.round(avgFat * 100.0) / 100.0);
        
        return analysis;
    }
    
    private Map<String, Object> calculateTrends(List<WorkoutSession> sessions, List<NutritionEntry> entries) {
        Map<String, Object> trends = new HashMap<>();
        
        // Workout frequency trend
        if (sessions.size() >= 14) {
            int firstHalfWorkouts = (int) sessions.stream()
                    .filter(s -> s.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                    .filter(s -> s.getCreatedAt().isBefore(LocalDateTime.now().minusDays(15)))
                    .count();
                    
            int secondHalfWorkouts = (int) sessions.stream()
                    .filter(s -> s.getCreatedAt().isAfter(LocalDateTime.now().minusDays(15)))
                    .count();
            
            String workoutTrend = secondHalfWorkouts > firstHalfWorkouts ? "increasing" : 
                                 secondHalfWorkouts < firstHalfWorkouts ? "decreasing" : "stable";
            trends.put("workoutFrequencyTrend", workoutTrend);
        }
        
        // Nutrition consistency trend
        if (entries.size() >= 14) {
            long entriesLastWeek = entries.stream()
                    .filter(e -> e.getDate().isAfter(LocalDate.now().minusDays(7)))
                    .count();
            
            long entriesWeekBefore = entries.stream()
                    .filter(e -> e.getDate().isAfter(LocalDate.now().minusDays(14)))
                    .filter(e -> e.getDate().isBefore(LocalDate.now().minusDays(7)))
                    .count();
            
            String nutritionTrend = entriesLastWeek > entriesWeekBefore ? "improving" : 
                                   entriesLastWeek < entriesWeekBefore ? "declining" : "stable";
            trends.put("nutritionConsistencyTrend", nutritionTrend);
        }
        
        return trends;
    }
    
    private Map<String, Object> analyzeGoalProgress(UserProfile profile, List<WorkoutSession> sessions, List<NutritionEntry> entries) {
        Map<String, Object> goalProgress = new HashMap<>();
        
        if (profile.getFitnessGoals() != null) {
            for (String goal : profile.getFitnessGoals()) {
                switch (goal.toLowerCase()) {
                    case "weight_loss":
                        goalProgress.put("weightLoss", analyzeWeightLossProgress(profile, sessions, entries));
                        break;
                    case "muscle_gain":
                        goalProgress.put("muscleGain", analyzeMuscleGainProgress(profile, sessions));
                        break;
                    case "endurance":
                        goalProgress.put("endurance", analyzeEnduranceProgress(sessions));
                        break;
                    case "strength":
                        goalProgress.put("strength", analyzeStrengthProgress(sessions));
                        break;
                }
            }
        }
        
        return goalProgress;
    }
    
    private Map<String, Object> suggestGoalAdjustments(UserProfile profile, Map<String, Object> analysisData) {
        Map<String, Object> adjustments = new HashMap<>();
        
        Map<String, Object> workoutAnalysis = (Map<String, Object>) analysisData.get("workoutAnalysis");
        Integer weeklyWorkouts = (Integer) workoutAnalysis.get("weeklyAverage");
        
        if (weeklyWorkouts != null && weeklyWorkouts < 2) {
            adjustments.put("workoutFrequency", "Consider setting a more achievable goal of 2 workouts per week initially");
        }
        
        // Check if current weight goal is realistic
        if (profile.getTargetWeight() != null && profile.getCurrentWeight() != null) {
            double weightDifference = Math.abs(profile.getCurrentWeight() - profile.getTargetWeight());
            if (weightDifference > 20) {
                adjustments.put("weightGoal", "Consider setting intermediate weight goals for better motivation");
            }
        }
        
        return adjustments;
    }
    
    private Map<String, Object> analyzeWeightLossProgress(UserProfile profile, List<WorkoutSession> sessions, List<NutritionEntry> entries) {
        Map<String, Object> progress = new HashMap<>();
        
        double totalCaloriesBurned = sessions.stream()
                .filter(s -> s.getTotalCaloriesBurned() != null)
                .mapToDouble(WorkoutSession::getTotalCaloriesBurned)
                .sum();
        
        double averageDailyCalories = entries.stream()
                .mapToDouble(NutritionEntry::getCalories)
                .average()
                .orElse(0.0);
        
        progress.put("totalCaloriesBurned", totalCaloriesBurned);
        progress.put("averageDailyCalories", averageDailyCalories);
        
        // Calculate estimated weight loss based on calorie deficit
        double tdee = profile.getTDEE() != null ? profile.getTDEE() : 2000;
        double dailyDeficit = tdee - averageDailyCalories + (totalCaloriesBurned / 30);
        double estimatedWeightLossPerWeek = (dailyDeficit * 7) / 3500; // 3500 calories = 1 pound
        
        progress.put("estimatedWeeklyWeightLoss", Math.round(estimatedWeightLossPerWeek * 100.0) / 100.0);
        
        return progress;
    }
    
    private Map<String, Object> analyzeMuscleGainProgress(UserProfile profile, List<WorkoutSession> sessions) {
        Map<String, Object> progress = new HashMap<>();
        
        long strengthSessions = sessions.stream()
                .filter(s -> "strength".equalsIgnoreCase(s.getSessionType()))
                .count();
        
        progress.put("strengthSessionsCount", strengthSessions);
        progress.put("recommendedWeeklyStrengthSessions", 3);
        progress.put("onTrack", strengthSessions >= 8); // 2+ per week over 4 weeks
        
        return progress;
    }
    
    private Map<String, Object> analyzeEnduranceProgress(List<WorkoutSession> sessions) {
        Map<String, Object> progress = new HashMap<>();
        
        List<WorkoutSession> cardioSessions = sessions.stream()
                .filter(s -> "cardio".equalsIgnoreCase(s.getSessionType()))
                .sorted(Comparator.comparing(WorkoutSession::getCreatedAt))
                .collect(Collectors.toList());
        
        if (cardioSessions.size() >= 2) {
            WorkoutSession first = cardioSessions.get(0);
            WorkoutSession latest = cardioSessions.get(cardioSessions.size() - 1);
            
            if (first.getDuration() != null && latest.getDuration() != null) {
                int improvement = latest.getDuration() - first.getDuration();
                progress.put("enduranceImprovement", improvement > 0 ? "improving" : "stable");
                progress.put("durationImprovement", improvement);
            }
        }
        
        progress.put("cardioSessionsCount", cardioSessions.size());
        return progress;
    }
    
    private Map<String, Object> analyzeStrengthProgress(List<WorkoutSession> sessions) {
        Map<String, Object> progress = new HashMap<>();
        
        long strengthSessions = sessions.stream()
                .filter(s -> "strength".equalsIgnoreCase(s.getSessionType()))
                .count();
        
        progress.put("strengthSessionsCount", strengthSessions);
        progress.put("progressTrend", strengthSessions >= 8 ? "good" : "needs_improvement");
        
        return progress;
    }
    
    
    private List<WorkoutSession> getRecentWorkoutSessions(String userId, int days) throws ExecutionException, InterruptedException {
        List<WorkoutSession> allSessions = workoutSessionRepository.findByUserId(userId);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        return allSessions.stream()
                .filter(session -> session.getCreatedAt().isAfter(cutoff))
                .collect(Collectors.toList());
    }
    
    private List<NutritionEntry> getRecentNutritionEntries(String userId, int days) throws ExecutionException, InterruptedException {
        List<NutritionEntry> allEntries = nutritionEntryRepository.findByUserId(userId);
        LocalDate cutoff = LocalDate.now().minusDays(days);
        
        return allEntries.stream()
                .filter(entry -> entry.getDate().isAfter(cutoff))
                .collect(Collectors.toList());
    }
}