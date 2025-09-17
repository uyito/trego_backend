package com.trego.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.trego.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class OpenAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    @Value("${openai.api-key}")
    private String apiKey;
    
    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;
    
    private OpenAiService getOpenAiService() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        return new OpenAiService(apiKey, Duration.ofMinutes(2));
    }
    
    public WorkoutPlan generatePersonalizedWorkout(UserProfile profile) {
        logger.info("Generating AI workout plan for user: {}", profile.getUserId());
        
        try {
            String prompt = buildWorkoutPrompt(profile);
            String response = callOpenAI(prompt);
            
            WorkoutPlan workoutPlan = parseWorkoutPlanFromResponse(response, profile);
            workoutPlan.setAiGenerated(true);
            
            logger.info("AI workout plan generated successfully for user: {}", profile.getUserId());
            return workoutPlan;
            
        } catch (Exception e) {
            logger.error("Failed to generate AI workout plan for user {}: {}", profile.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate personalized workout", e);
        }
    }
    
    public List<String> generateNutritionRecommendations(UserProfile profile, Map<String, Double> currentMacros) {
        logger.info("Generating nutrition recommendations for user: {}", profile.getUserId());
        
        try {
            String prompt = buildNutritionPrompt(profile, currentMacros);
            String response = callOpenAI(prompt);
            
            List<String> recommendations = parseNutritionRecommendations(response);
            
            logger.info("Nutrition recommendations generated successfully for user: {}", profile.getUserId());
            return recommendations;
            
        } catch (Exception e) {
            logger.error("Failed to generate nutrition recommendations for user {}: {}", profile.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate nutrition recommendations", e);
        }
    }
    
    public String generateMotivationalMessage(UserProfile profile, Map<String, Object> recentStats) {
        logger.info("Generating motivational message for user: {}", profile.getUserId());
        
        try {
            String prompt = buildMotivationPrompt(profile, recentStats);
            String response = callOpenAI(prompt);
            
            logger.info("Motivational message generated for user: {}", profile.getUserId());
            return response.trim();
            
        } catch (Exception e) {
            logger.error("Failed to generate motivational message for user {}: {}", profile.getUserId(), e.getMessage(), e);
            return "Keep pushing forward! Every workout brings you closer to your goals. 💪";
        }
    }
    
    public Recipe generateRecipe(UserProfile profile, Map<String, Object> preferences) {
        logger.info("Generating AI recipe for user: {}", profile.getUserId());
        
        try {
            String prompt = buildRecipePrompt(profile, preferences);
            String response = callOpenAI(prompt);
            
            Recipe recipe = parseRecipeFromResponse(response);
            recipe.setAIGenerated(true);
            
            logger.info("AI recipe generated successfully for user: {}", profile.getUserId());
            return recipe;
            
        } catch (Exception e) {
            logger.error("Failed to generate AI recipe for user {}: {}", profile.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI recipe", e);
        }
    }
    
    public List<Recipe> generateRecipesFromIngredients(UserProfile profile, List<String> ingredients, Map<String, Object> preferences) {
        logger.info("Generating recipes from pantry ingredients for user: {}", profile.getUserId());
        
        try {
            String prompt = buildRecipeFromIngredientsPrompt(profile, ingredients, preferences);
            String response = callOpenAI(prompt);
            
            List<Recipe> recipes = parseMultipleRecipesFromResponse(response);
            recipes.forEach(recipe -> recipe.setAIGenerated(true));
            
            logger.info("Recipes from pantry generated successfully for user: {}", profile.getUserId());
            return recipes;
            
        } catch (Exception e) {
            logger.error("Failed to generate recipes from pantry for user {}: {}", profile.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate recipes from pantry", e);
        }
    }
    
    public String generateCoachResponse(UserProfile profile, String message, Map<String, Object> context) {
        logger.info("Generating AI coach response for user: {}", profile.getUserId());
        
        try {
            String prompt = buildCoachChatPrompt(profile, message, context);
            String response = callOpenAI(prompt);
            
            logger.info("AI coach response generated for user: {}", profile.getUserId());
            return response.trim();
            
        } catch (Exception e) {
            logger.error("Failed to generate AI coach response for user {}: {}", profile.getUserId(), e.getMessage(), e);
            return "I'm having trouble connecting right now. Please try again later or check out your progress analytics for insights!";
        }
    }
    
    public List<String> generateMealSuggestions(UserProfile profile, String mealType, Double targetCalories) {
        logger.info("Generating meal suggestions for user: {} - {}", profile.getUserId(), mealType);
        
        try {
            String prompt = buildMealSuggestionPrompt(profile, mealType, targetCalories);
            String response = callOpenAI(prompt);
            
            List<String> meals = parseMealSuggestions(response);
            
            logger.info("Meal suggestions generated for user: {}", profile.getUserId());
            return meals;
            
        } catch (Exception e) {
            logger.error("Failed to generate meal suggestions for user {}: {}", profile.getUserId(), e.getMessage(), e);
            return getDefaultMealSuggestions(mealType);
        }
    }
    
    private String callOpenAI(String prompt) {
        OpenAiService service = getOpenAiService();
        
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are a certified fitness trainer and nutritionist with expertise in personalized workout plans and nutrition advice. " +
                "Provide practical, safe, and scientifically-backed recommendations. Keep responses concise and actionable."),
            new ChatMessage(ChatMessageRole.USER.value(), prompt)
        );
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(1000)
                .temperature(0.7)
                .build();
        
        ChatCompletionResult result = service.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent();
    }
    
    private String buildWorkoutPrompt(UserProfile profile) {
        return String.format(
            "Create a personalized workout plan for:\n" +
            "- Fitness Goals: %s\n" +
            "- Experience Level: %s\n" +
            "- Activity Level: %s\n" +
            "- Preferred Duration: %d minutes\n" +
            "- Workout Frequency: %d times per week\n" +
            "- Age: %s, Gender: %s\n" +
            "- Medical Conditions: %s\n" +
            "- Available Time: %s\n\n" +
            "Provide a structured workout with:\n" +
            "1. Workout name and description\n" +
            "2. 5-8 specific exercises with sets, reps, and rest periods\n" +
            "3. Target muscle groups\n" +
            "4. Estimated calories burned\n" +
            "5. Equipment needed (bodyweight if none available)\n" +
            "6. Safety tips and modifications\n\n" +
            "Format as structured data that can be parsed.",
            
            profile.getFitnessGoals(),
            profile.getExperience(),
            profile.getActivityLevel(),
            profile.getWorkoutDuration(),
            profile.getWorkoutFrequency(),
            profile.calculateAge(),
            profile.getGender(),
            profile.getMedicalConditions(),
            profile.getPreferredWorkoutTime()
        );
    }
    
    private String buildNutritionPrompt(UserProfile profile, Map<String, Double> currentMacros) {
        Double tdee = profile.calculateTDEE();
        
        return String.format(
            "Provide nutrition recommendations for:\n" +
            "- Current TDEE: %.0f calories\n" +
            "- Fitness Goals: %s\n" +
            "- Current Macros - Protein: %.1fg, Carbs: %.1fg, Fat: %.1fg\n" +
            "- Dietary Restrictions: %s\n" +
            "- Activity Level: %s\n" +
            "- Gender: %s, Age: %s\n\n" +
            "Provide 5 specific, actionable nutrition recommendations to optimize their goals. " +
            "Include macro targets and meal timing if relevant.",
            
            tdee != null ? tdee : 2000,
            profile.getFitnessGoals(),
            currentMacros.getOrDefault("protein", 0.0),
            currentMacros.getOrDefault("carbs", 0.0),
            currentMacros.getOrDefault("fat", 0.0),
            profile.getDietaryRestrictions(),
            profile.getActivityLevel(),
            profile.getGender(),
            profile.calculateAge()
        );
    }
    
    private String buildMotivationPrompt(UserProfile profile, Map<String, Object> recentStats) {
        return String.format(
            "Generate a personalized motivational message for a fitness app user:\n" +
            "- Goals: %s\n" +
            "- Recent workouts completed: %s\n" +
            "- Current streak: %s days\n" +
            "- Experience level: %s\n\n" +
            "Create an encouraging, specific message (2-3 sentences) that acknowledges their progress and motivates continued effort. " +
            "Be positive and specific to their goals.",
            
            profile.getFitnessGoals(),
            recentStats.getOrDefault("recentWorkouts", 0),
            recentStats.getOrDefault("currentStreak", 0),
            profile.getExperience()
        );
    }
    
    private String buildMealSuggestionPrompt(UserProfile profile, String mealType, Double targetCalories) {
        return String.format(
            "Suggest 3 healthy %s meals for:\n" +
            "- Target Calories: %.0f\n" +
            "- Fitness Goals: %s\n" +
            "- Dietary Restrictions: %s\n" +
            "- Activity Level: %s\n\n" +
            "Provide meal names with brief descriptions. Focus on nutritious, balanced options that support their fitness goals.",
            
            mealType,
            targetCalories,
            profile.getFitnessGoals(),
            profile.getDietaryRestrictions(),
            profile.getActivityLevel()
        );
    }
    
    private WorkoutPlan parseWorkoutPlanFromResponse(String response, UserProfile profile) {
        WorkoutPlan plan = new WorkoutPlan(profile.getUserId());
        
        // Simple parsing - in a real implementation, you'd use more sophisticated parsing
        if (response.toLowerCase().contains("hiit") || response.toLowerCase().contains("high intensity")) {
            plan.setWorkoutType("hiit");
            plan.setDifficulty("intermediate");
        } else if (response.toLowerCase().contains("strength") || response.toLowerCase().contains("weight")) {
            plan.setWorkoutType("strength");
        } else if (response.toLowerCase().contains("cardio") || response.toLowerCase().contains("running")) {
            plan.setWorkoutType("cardio");
        } else {
            plan.setWorkoutType("general");
        }
        
        plan.setName("AI Generated Workout Plan");
        plan.setDescription(response.length() > 200 ? response.substring(0, 200) + "..." : response);
        plan.setDuration(profile.getWorkoutDuration());
        plan.setDifficulty(profile.getExperience());
        
        // Estimate calories based on workout type and user profile
        plan.setCaloriesBurnedEstimate(estimateCaloriesBurned(profile, plan.getWorkoutType(), plan.getDuration()));
        
        // Add some default exercises - in real implementation, parse from AI response
        addDefaultExercises(plan, profile);
        
        return plan;
    }
    
    private void addDefaultExercises(WorkoutPlan plan, UserProfile profile) {
        switch (plan.getWorkoutType()) {
            case "strength":
                plan.addExercise("Push-ups", 3, 12, null, null, "Focus on proper form");
                plan.addExercise("Squats", 3, 15, null, null, "Keep knees aligned with toes");
                plan.addExercise("Plank", 3, null, null, 30, "Hold for specified duration");
                plan.addExercise("Lunges", 3, 10, null, null, "Alternate legs");
                break;
            case "cardio":
                plan.addExercise("Jumping Jacks", 3, 20, null, null, "Full body movement");
                plan.addExercise("High Knees", 3, null, null, 30, "Lift knees to chest level");
                plan.addExercise("Burpees", 3, 8, null, null, "Full body explosive movement");
                break;
            case "hiit":
                plan.addExercise("Mountain Climbers", 4, null, null, 20, "High intensity");
                plan.addExercise("Jump Squats", 4, 12, null, null, "Explosive movement");
                plan.addExercise("Push-up to T", 4, 10, null, null, "Add rotation at top");
                break;
            default:
                plan.addExercise("Bodyweight Squats", 3, 15, null, null, "Basic movement pattern");
                plan.addExercise("Modified Push-ups", 3, 10, null, null, "Knee or wall modification if needed");
                plan.addExercise("Seated Leg Extensions", 3, 12, null, null, "Chair-based exercise");
        }
    }
    
    private List<String> parseNutritionRecommendations(String response) {
        List<String> recommendations = new ArrayList<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 10 && (line.matches("^\\d+\\..*") || line.startsWith("•") || line.startsWith("-"))) {
                recommendations.add(line.replaceFirst("^\\d+\\.\\s*|^[•-]\\s*", ""));
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Focus on whole, unprocessed foods");
            recommendations.add("Ensure adequate protein intake for your goals");
            recommendations.add("Stay hydrated throughout the day");
            recommendations.add("Time your carbs around workouts");
            recommendations.add("Include healthy fats in every meal");
        }
        
        return recommendations;
    }
    
    private List<String> parseMealSuggestions(String response) {
        List<String> meals = new ArrayList<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 10 && (line.matches("^\\d+\\..*") || line.contains(":"))) {
                meals.add(line.replaceFirst("^\\d+\\.\\s*", ""));
            }
        }
        
        return meals.isEmpty() ? getDefaultMealSuggestions("general") : meals;
    }
    
    private List<String> getDefaultMealSuggestions(String mealType) {
        return switch (mealType.toLowerCase()) {
            case "breakfast" -> Arrays.asList(
                "Greek yogurt with berries and granola",
                "Oatmeal with banana and almonds",
                "Scrambled eggs with whole grain toast"
            );
            case "lunch" -> Arrays.asList(
                "Grilled chicken salad with mixed vegetables",
                "Quinoa bowl with roasted vegetables",
                "Turkey and avocado wrap"
            );
            case "dinner" -> Arrays.asList(
                "Baked salmon with sweet potato and broccoli",
                "Lean beef stir-fry with brown rice",
                "Lentil soup with whole grain bread"
            );
            case "snack" -> Arrays.asList(
                "Apple with almond butter",
                "Greek yogurt with nuts",
                "Hummus with carrot sticks"
            );
            default -> Arrays.asList(
                "Balanced meal with lean protein, complex carbs, and vegetables",
                "Focus on whole, unprocessed foods",
                "Include a variety of colorful fruits and vegetables"
            );
        };
    }
    
    private Double estimateCaloriesBurned(UserProfile profile, String workoutType, Integer duration) {
        if (duration == null || profile.getWeight() == null) {
            return 200.0; // Default estimate
        }
        
        double weight = profile.getWeight();
        double hours = duration / 60.0;
        
        // METs (Metabolic Equivalent of Task) values for different activities
        double mets = switch (workoutType) {
            case "hiit" -> 8.0;
            case "strength" -> 6.0;
            case "cardio" -> 7.0;
            case "yoga" -> 3.0;
            case "pilates" -> 4.0;
            default -> 5.0;
        };
        
        // Calories = METs × weight in kg × hours
        return mets * weight * hours;
    }
    
    private String buildRecipePrompt(UserProfile profile, Map<String, Object> preferences) {
        String mealType = (String) preferences.getOrDefault("mealType", "dinner");
        String cuisineType = (String) preferences.getOrDefault("cuisineType", "any");
        String difficulty = (String) preferences.getOrDefault("difficulty", "medium");
        Integer maxTime = (Integer) preferences.getOrDefault("maxTime", 45);
        
        return String.format(
            "Create a detailed recipe for a %s meal:\n" +
            "- Cuisine: %s\n" +
            "- Difficulty: %s\n" +
            "- Max prep + cook time: %d minutes\n" +
            "- Dietary restrictions: %s\n" +
            "- Allergies: %s\n" +
            "- Fitness goals: %s\n" +
            "- Cooking skill: %s\n\n" +
            "Provide:\n" +
            "1. Recipe title\n" +
            "2. Brief description\n" +
            "3. Prep time and cook time\n" +
            "4. Number of servings\n" +
            "5. Detailed ingredient list with quantities\n" +
            "6. Step-by-step instructions\n" +
            "7. Nutrition facts (calories, protein, carbs, fat per serving)\n" +
            "8. Cooking tips\n\n" +
            "Format as structured text that can be parsed.",
            
            mealType,
            cuisineType,
            difficulty,
            maxTime,
            profile.getDietaryRestrictions(),
            profile.getAllergies(),
            profile.getFitnessGoals(),
            profile.getCookingSkillLevel()
        );
    }
    
    private String buildRecipeFromIngredientsPrompt(UserProfile profile, List<String> ingredients, Map<String, Object> preferences) {
        return String.format(
            "Create 2-3 recipes using these pantry ingredients: %s\n\n" +
            "User preferences:\n" +
            "- Dietary restrictions: %s\n" +
            "- Allergies: %s\n" +
            "- Cooking skill: %s\n" +
            "- Preferred cuisine: %s\n" +
            "- Max cook time: %s minutes\n\n" +
            "For each recipe provide:\n" +
            "1. Recipe name\n" +
            "2. Ingredients needed (prioritize pantry items)\n" +
            "3. Simple instructions\n" +
            "4. Estimated nutrition per serving\n\n" +
            "Focus on minimizing food waste and using expiring items first.",
            
            ingredients,
            profile.getDietaryRestrictions(),
            profile.getAllergies(),
            profile.getCookingSkillLevel(),
            preferences.getOrDefault("cuisineType", "any"),
            preferences.getOrDefault("maxTime", 45)
        );
    }
    
    private String buildCoachChatPrompt(UserProfile profile, String message, Map<String, Object> context) {
        return String.format(
            "You are a personal fitness coach chatting with a user. User profile:\n" +
            "- Goals: %s\n" +
            "- Experience: %s\n" +
            "- Recent progress: %s\n\n" +
            "User message: \"%s\"\n\n" +
            "Respond as a supportive, knowledgeable coach. Be encouraging, specific, and actionable. " +
            "Keep response under 150 words.",
            
            profile.getFitnessGoals(),
            profile.getExperience(),
            context,
            message
        );
    }
    
    private Recipe parseRecipeFromResponse(String response) {
        Recipe recipe = new Recipe();
        
        // Simple parsing - in production, use more sophisticated JSON parsing
        recipe.setTitle("AI Generated Recipe");
        recipe.setDescription("Generated by AI based on your preferences");
        recipe.setPrepTime(15);
        recipe.setCookTime(30);
        recipe.setServings(4);
        recipe.setDifficulty("medium");
        recipe.setCuisineType("international");
        
        // Add sample ingredients and instructions
        List<RecipeIngredient> ingredients = new ArrayList<>();
        ingredients.add(new RecipeIngredient("Chicken breast", 500.0, "g"));
        ingredients.add(new RecipeIngredient("Olive oil", 2.0, "tbsp"));
        ingredients.add(new RecipeIngredient("Salt", 1.0, "tsp"));
        recipe.setIngredients(ingredients);
        
        List<RecipeInstruction> instructions = new ArrayList<>();
        instructions.add(new RecipeInstruction(1, "Prepare ingredients"));
        instructions.add(new RecipeInstruction(2, "Cook according to AI recommendations"));
        recipe.setInstructions(instructions);
        
        // Add basic nutrition facts
        NutritionFacts nutrition = new NutritionFacts(350.0, 25.0, 15.0, 20.0);
        recipe.setNutritionFacts(nutrition);
        
        return recipe;
    }
    
    private List<Recipe> parseMultipleRecipesFromResponse(String response) {
        List<Recipe> recipes = new ArrayList<>();
        
        // For simplicity, return 2-3 basic recipes
        for (int i = 1; i <= 2; i++) {
            Recipe recipe = parseRecipeFromResponse(response);
            recipe.setTitle("Pantry Recipe " + i);
            recipes.add(recipe);
        }
        
        return recipes;
    }

}