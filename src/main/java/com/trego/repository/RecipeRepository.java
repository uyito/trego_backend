package com.trego.repository;

import com.trego.model.Recipe;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class RecipeRepository extends FirestoreRepository<Recipe> {
    
    public RecipeRepository() {
        super("recipes", Recipe::fromFirestoreMap);
    }
    
    public List<Recipe> findByCreatedBy(String userId) throws ExecutionException, InterruptedException {
        return findByField("createdBy", userId);
    }
    
    public List<Recipe> findPublicRecipes() throws ExecutionException, InterruptedException {
        return findByField("isPublic", true);
    }
    
    public List<Recipe> findByMealType(String mealType) throws ExecutionException, InterruptedException {
        return findByField("mealType", mealType);
    }
    
    public List<Recipe> findByCuisineType(String cuisineType) throws ExecutionException, InterruptedException {
        return findByField("cuisineType", cuisineType);
    }
    
    public List<Recipe> findByDifficulty(String difficulty) throws ExecutionException, InterruptedException {
        return findByField("difficulty", difficulty);
    }
    
    public List<Recipe> findAIGenerated() throws ExecutionException, InterruptedException {
        return findByField("isAIGenerated", true);
    }
    
    public List<Recipe> searchRecipes(String query, Map<String, Object> filters) throws ExecutionException, InterruptedException {
        List<Recipe> allRecipes = findPublicRecipes();
        
        return allRecipes.stream()
                .filter(recipe -> matchesQuery(recipe, query))
                .filter(recipe -> matchesFilters(recipe, filters))
                .collect(Collectors.toList());
    }
    
    public List<Recipe> findPopularRecipes(int limit) throws ExecutionException, InterruptedException {
        List<Recipe> publicRecipes = findPublicRecipes();
        
        return publicRecipes.stream()
                .sorted((a, b) -> {
                    double scoreA = calculatePopularityScore(a);
                    double scoreB = calculatePopularityScore(b);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public void deleteAllByUserId(String userId) throws ExecutionException, InterruptedException {
        List<Recipe> userRecipes = findByCreatedBy(userId);
        for (Recipe recipe : userRecipes) {
            deleteById(recipe.getId());
        }
    }
    
    private boolean matchesQuery(Recipe recipe, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        return (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(lowerQuery)) ||
               (recipe.getDescription() != null && recipe.getDescription().toLowerCase().contains(lowerQuery)) ||
               (recipe.getTags() != null && recipe.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))) ||
               (recipe.getIngredients() != null && recipe.getIngredients().stream()
                   .anyMatch(ingredient -> ingredient.getName().toLowerCase().contains(lowerQuery)));
    }
    
    private boolean matchesFilters(Recipe recipe, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String key = filter.getKey();
            Object value = filter.getValue();
            
            switch (key) {
                case "mealType":
                    if (value != null && !value.equals(recipe.getMealType())) {
                        return false;
                    }
                    break;
                case "cuisineType":
                    if (value != null && !value.equals(recipe.getCuisineType())) {
                        return false;
                    }
                    break;
                case "difficulty":
                    if (value != null && !value.equals(recipe.getDifficulty())) {
                        return false;
                    }
                    break;
                case "maxPrepTime":
                    if (value instanceof Number && recipe.getPrepTime() != null) {
                        if (recipe.getPrepTime() > ((Number) value).intValue()) {
                            return false;
                        }
                    }
                    break;
                case "maxCookTime":
                    if (value instanceof Number && recipe.getCookTime() != null) {
                        if (recipe.getCookTime() > ((Number) value).intValue()) {
                            return false;
                        }
                    }
                    break;
                case "isAIGenerated":
                    if (value instanceof Boolean && !value.equals(recipe.isAIGenerated())) {
                        return false;
                    }
                    break;
            }
        }
        
        return true;
    }
    
    private double calculatePopularityScore(Recipe recipe) {
        double score = 0.0;
        
        if (recipe.getRatings() != null && recipe.getRatings().getAverage() != null) {
            score += recipe.getRatings().getAverage() * 20;
        }
        
        if (recipe.getMadeCount() != null) {
            score += recipe.getMadeCount() * 2;
        }
        
        if (recipe.getSaveCount() != null) {
            score += recipe.getSaveCount() * 1;
        }
        
        if (recipe.getViewCount() != null) {
            score += recipe.getViewCount() * 0.1;
        }
        
        return score;
    }
}