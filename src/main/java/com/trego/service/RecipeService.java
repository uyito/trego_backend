package com.trego.service;

import com.trego.model.*;
import com.trego.repository.RecipeRepository;
import com.trego.repository.UserProfileRepository;
import com.trego.repository.PantryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    
    @Autowired
    private RecipeRepository recipeRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private PantryRepository pantryRepository;
    
    @Autowired
    private OpenAIService openAIService;
    
    public Recipe generateAIRecipe(String userId, Map<String, Object> preferences) throws ExecutionException, InterruptedException {
        logger.info("Generating AI recipe for user: {}", userId);
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User profile not found");
        }
        
        UserProfile profile = profileOpt.get();
        Recipe aiRecipe = openAIService.generateRecipe(profile, preferences);
        aiRecipe.setCreatedBy(userId);
        aiRecipe.setAIGenerated(true);
        
        Recipe savedRecipe = recipeRepository.save(aiRecipe);
        logger.info("AI recipe generated and saved: {}", savedRecipe.getId());
        
        return savedRecipe;
    }
    
    public List<Recipe> getPersonalizedRecommendations(String userId, int limit) throws ExecutionException, InterruptedException {
        logger.info("Getting personalized recipe recommendations for user: {}", userId);
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return getPopularRecipes(limit);
        }
        
        UserProfile profile = profileOpt.get();
        List<Recipe> allPublicRecipes = recipeRepository.findPublicRecipes();
        
        return allPublicRecipes.stream()
                .filter(recipe -> isRecipeSuitable(recipe, profile))
                .sorted((a, b) -> Double.compare(calculateRecipeScore(b, profile), calculateRecipeScore(a, profile)))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<Recipe> searchRecipes(String query, Map<String, Object> filters, int limit) throws ExecutionException, InterruptedException {
        logger.info("Searching recipes with query: {} and filters: {}", query, filters);
        
        List<Recipe> results = recipeRepository.searchRecipes(query, filters);
        
        return results.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<Recipe> generateRecipeFromPantry(String userId, Map<String, Object> preferences) throws ExecutionException, InterruptedException {
        logger.info("Generating recipes from pantry for user: {}", userId);
        
        List<PantryItem> pantryItems = pantryRepository.findByUserId(userId);
        if (pantryItems.isEmpty()) {
            throw new IllegalArgumentException("No pantry items found");
        }
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User profile not found");
        }
        
        UserProfile profile = profileOpt.get();
        List<String> availableIngredients = pantryItems.stream()
                .filter(item -> !item.isFinished() && !item.isExpired())
                .map(PantryItem::getName)
                .collect(Collectors.toList());
        
        return openAIService.generateRecipesFromIngredients(profile, availableIngredients, preferences);
    }
    
    public List<Recipe> getMealPlanSuggestions(String userId, String mealType, LocalDate date) throws ExecutionException, InterruptedException {
        logger.info("Getting meal plan suggestions for user: {} - {} on {}", userId, mealType, date);
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("User profile not found");
        }
        
        UserProfile profile = profileOpt.get();
        
        // Get recipes that match the meal type and user preferences
        List<Recipe> suitableRecipes = recipeRepository.findByMealType(mealType);
        
        return suitableRecipes.stream()
                .filter(recipe -> isRecipeSuitable(recipe, profile))
                .sorted((a, b) -> Double.compare(calculateRecipeScore(b, profile), calculateRecipeScore(a, profile)))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    public Recipe saveRecipe(String userId, String recipeId) throws ExecutionException, InterruptedException {
        logger.info("User {} saving recipe: {}", userId, recipeId);
        
        Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
        if (recipeOpt.isEmpty()) {
            throw new IllegalArgumentException("Recipe not found");
        }
        
        Recipe recipe = recipeOpt.get();
        recipe.incrementSaveCount();
        
        return recipeRepository.update(recipe);
    }
    
    public Recipe rateRecipe(String recipeId, Double rating) throws ExecutionException, InterruptedException {
        logger.info("Rating recipe: {} with rating: {}", recipeId, rating);
        
        Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
        if (recipeOpt.isEmpty()) {
            throw new IllegalArgumentException("Recipe not found");
        }
        
        Recipe recipe = recipeOpt.get();
        recipe.addRating(rating);
        
        return recipeRepository.update(recipe);
    }
    
    public Recipe markRecipeMade(String recipeId) throws ExecutionException, InterruptedException {
        logger.info("Marking recipe as made: {}", recipeId);
        
        Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
        if (recipeOpt.isEmpty()) {
            throw new IllegalArgumentException("Recipe not found");
        }
        
        Recipe recipe = recipeOpt.get();
        recipe.incrementMadeCount();
        
        return recipeRepository.update(recipe);
    }
    
    public List<Recipe> getUserRecipes(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting recipes for user: {}", userId);
        return recipeRepository.findByCreatedBy(userId);
    }
    
    public List<Recipe> getPopularRecipes(int limit) throws ExecutionException, InterruptedException {
        logger.info("Getting popular recipes with limit: {}", limit);
        
        List<Recipe> publicRecipes = recipeRepository.findPublicRecipes();
        
        return publicRecipes.stream()
                .sorted((a, b) -> {
                    double scoreA = (a.getRatings().getAverage() * 0.6) + (a.getMadeCount() * 0.3) + (a.getSaveCount() * 0.1);
                    double scoreB = (b.getRatings().getAverage() * 0.6) + (b.getMadeCount() * 0.3) + (b.getSaveCount() * 0.1);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private boolean isRecipeSuitable(Recipe recipe, UserProfile profile) {
        // Check dietary restrictions
        if (profile.getDietaryRestrictions() != null && recipe.getTags() != null) {
            for (String restriction : profile.getDietaryRestrictions()) {
                if (recipe.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(restriction.toLowerCase()))) {
                    return false;
                }
            }
        }
        
        // Check allergies
        if (profile.getAllergies() != null && recipe.getIngredients() != null) {
            for (String allergy : profile.getAllergies()) {
                if (recipe.getIngredients().stream().anyMatch(ingredient -> 
                    ingredient.getName().toLowerCase().contains(allergy.toLowerCase()))) {
                    return false;
                }
            }
        }
        
        // Check cuisine preferences
        if (profile.getCuisinePreferences() != null && recipe.getCuisineType() != null) {
            return profile.getCuisinePreferences().stream()
                    .anyMatch(cuisine -> cuisine.equalsIgnoreCase(recipe.getCuisineType()));
        }
        
        return true;
    }
    
    private double calculateRecipeScore(Recipe recipe, UserProfile profile) {
        double score = 0.0;
        
        // Base score from ratings and popularity
        score += recipe.getRatings().getAverage() * 20;
        score += Math.min(recipe.getMadeCount() / 10.0, 15);
        score += Math.min(recipe.getSaveCount() / 5.0, 10);
        
        // Bonus for matching preferences
        if (profile.getCookingSkillLevel() != null && profile.getCookingSkillLevel().equals(recipe.getDifficulty())) {
            score += 15;
        }
        
        // Bonus for matching cuisine preferences
        if (profile.getCuisinePreferences() != null && recipe.getCuisineType() != null) {
            if (profile.getCuisinePreferences().contains(recipe.getCuisineType())) {
                score += 10;
            }
        }
        
        // Penalty for very long prep time if user prefers quick meals
        if (recipe.getTotalTime() != null && recipe.getTotalTime() > 60) {
            score -= 5;
        }
        
        return score;
    }
}