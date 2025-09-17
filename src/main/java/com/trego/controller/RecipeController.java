package com.trego.controller;

import com.trego.dto.ApiResponse;
import com.trego.model.Recipe;
import com.trego.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recipes")
public class RecipeController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);
    
    @Autowired
    private RecipeService recipeService;
    
    @PostMapping("/generate-ai")
    public ResponseEntity<ApiResponse<Recipe>> generateAIRecipe(
            @RequestBody Map<String, Object> preferences,
            Principal principal) {
        
        try {
            logger.info("AI recipe generation request from user: {}", principal.getName());
            
            Recipe recipe = recipeService.generateAIRecipe(principal.getName(), preferences);
            
            return ResponseEntity.ok(ApiResponse.success("AI recipe generated successfully", recipe));
            
        } catch (Exception e) {
            logger.error("AI recipe generation failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate AI recipe", "RECIPE_GEN_001"));
        }
    }
    
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<Recipe>>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        
        try {
            logger.info("Recipe recommendations request from user: {}", principal.getName());
            
            List<Recipe> recipes = recipeService.getPersonalizedRecommendations(principal.getName(), limit);
            
            return ResponseEntity.ok(ApiResponse.success("Recommendations retrieved successfully", recipes));
            
        } catch (Exception e) {
            logger.error("Recipe recommendations failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get recommendations", "RECIPE_REC_001"));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Recipe>>> searchRecipes(
            @RequestParam String query,
            @RequestParam Map<String, Object> filters,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            logger.info("Recipe search request: {} with filters: {}", query, filters);
            
            List<Recipe> recipes = recipeService.searchRecipes(query, filters, limit);
            
            return ResponseEntity.ok(ApiResponse.success("Search completed successfully", recipes));
            
        } catch (Exception e) {
            logger.error("Recipe search failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search failed", "RECIPE_SEARCH_001"));
        }
    }
    
    @PostMapping("/from-pantry")
    public ResponseEntity<ApiResponse<List<Recipe>>> generateRecipeFromPantry(
            @RequestBody Map<String, Object> preferences,
            Principal principal) {
        
        try {
            logger.info("Generate recipe from pantry request from user: {}", principal.getName());
            
            List<Recipe> recipes = recipeService.generateRecipeFromPantry(principal.getName(), preferences);
            
            return ResponseEntity.ok(ApiResponse.success("Recipes generated from pantry items", recipes));
            
        } catch (Exception e) {
            logger.error("Generate recipe from pantry failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate recipes from pantry", "RECIPE_PANTRY_001"));
        }
    }
    
    @PostMapping("/{recipeId}/save")
    public ResponseEntity<ApiResponse<Recipe>> saveRecipe(
            @PathVariable String recipeId,
            Principal principal) {
        
        try {
            logger.info("Save recipe request from user: {} for recipe: {}", principal.getName(), recipeId);
            
            Recipe recipe = recipeService.saveRecipe(principal.getName(), recipeId);
            
            return ResponseEntity.ok(ApiResponse.success("Recipe saved successfully", recipe));
            
        } catch (Exception e) {
            logger.error("Save recipe failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to save recipe", "RECIPE_SAVE_001"));
        }
    }
    
    @PostMapping("/{recipeId}/rate")
    public ResponseEntity<ApiResponse<Recipe>> rateRecipe(
            @PathVariable String recipeId,
            @RequestBody Map<String, Object> request,
            Principal principal) {
        
        try {
            logger.info("Rate recipe request from user: {} for recipe: {}", principal.getName(), recipeId);
            
            Double rating = ((Number) request.get("rating")).doubleValue();
            Recipe recipe = recipeService.rateRecipe(recipeId, rating);
            
            return ResponseEntity.ok(ApiResponse.success("Recipe rated successfully", recipe));
            
        } catch (Exception e) {
            logger.error("Rate recipe failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to rate recipe", "RECIPE_RATE_001"));
        }
    }
    
    @PostMapping("/{recipeId}/made")
    public ResponseEntity<ApiResponse<Recipe>> markRecipeMade(
            @PathVariable String recipeId,
            Principal principal) {
        
        try {
            logger.info("Mark recipe made request from user: {} for recipe: {}", principal.getName(), recipeId);
            
            Recipe recipe = recipeService.markRecipeMade(recipeId);
            
            return ResponseEntity.ok(ApiResponse.success("Recipe marked as made", recipe));
            
        } catch (Exception e) {
            logger.error("Mark recipe made failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to mark recipe as made", "RECIPE_MADE_001"));
        }
    }
    
    @GetMapping("/meal-plan")
    public ResponseEntity<ApiResponse<List<Recipe>>> getMealPlanSuggestions(
            @RequestParam String mealType,
            @RequestParam(required = false) String date,
            Principal principal) {
        
        try {
            logger.info("Meal plan suggestions request from user: {} for meal type: {}", principal.getName(), mealType);
            
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            List<Recipe> recipes = recipeService.getMealPlanSuggestions(principal.getName(), mealType, targetDate);
            
            return ResponseEntity.ok(ApiResponse.success("Meal plan suggestions retrieved", recipes));
            
        } catch (Exception e) {
            logger.error("Meal plan suggestions failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get meal plan suggestions", "MEAL_PLAN_001"));
        }
    }
    
    @GetMapping("/my-recipes")
    public ResponseEntity<ApiResponse<List<Recipe>>> getUserRecipes(Principal principal) {
        try {
            logger.info("Get user recipes request from user: {}", principal.getName());
            
            List<Recipe> recipes = recipeService.getUserRecipes(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("User recipes retrieved successfully", recipes));
            
        } catch (Exception e) {
            logger.error("Get user recipes failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get user recipes", "USER_RECIPES_001"));
        }
    }
    
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<Recipe>>> getPopularRecipes(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            logger.info("Get popular recipes request with limit: {}", limit);
            
            List<Recipe> recipes = recipeService.getPopularRecipes(limit);
            
            return ResponseEntity.ok(ApiResponse.success("Popular recipes retrieved successfully", recipes));
            
        } catch (Exception e) {
            logger.error("Get popular recipes failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get popular recipes", "POPULAR_RECIPES_001"));
        }
    }
}