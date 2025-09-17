package com.trego.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe extends BaseEntity {
    
    private String title;
    private String description;
    private String imageUrl;
    private String createdBy;
    private boolean isAIGenerated;
    private String difficulty;
    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private List<RecipeIngredient> ingredients;
    private List<RecipeInstruction> instructions;
    private NutritionFacts nutritionFacts;
    private List<String> tags;
    private RecipeRatings ratings;
    private String cuisineType;
    private String mealType;
    private boolean isPublic;
    private Integer viewCount;
    private Integer saveCount;
    private Integer madeCount;
    
    public Recipe() {
        super();
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.ratings = new RecipeRatings();
        this.isPublic = false;
        this.viewCount = 0;
        this.saveCount = 0;
        this.madeCount = 0;
    }
    
    public Recipe(String title, String description) {
        this();
        this.title = title;
        this.description = description;
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("title", title);
        map.put("description", description);
        map.put("imageUrl", imageUrl);
        map.put("createdBy", createdBy);
        map.put("isAIGenerated", isAIGenerated);
        map.put("difficulty", difficulty);
        map.put("prepTime", prepTime);
        map.put("cookTime", cookTime);
        map.put("servings", servings);
        map.put("cuisineType", cuisineType);
        map.put("mealType", mealType);
        map.put("isPublic", isPublic);
        map.put("viewCount", viewCount);
        map.put("saveCount", saveCount);
        map.put("madeCount", madeCount);
        map.put("tags", tags);
        
        if (ingredients != null) {
            List<Map<String, Object>> ingredientMaps = new ArrayList<>();
            for (RecipeIngredient ingredient : ingredients) {
                ingredientMaps.add(ingredient.toFirestoreMap());
            }
            map.put("ingredients", ingredientMaps);
        }
        
        if (instructions != null) {
            List<Map<String, Object>> instructionMaps = new ArrayList<>();
            for (RecipeInstruction instruction : instructions) {
                instructionMaps.add(instruction.toFirestoreMap());
            }
            map.put("instructions", instructionMaps);
        }
        
        if (nutritionFacts != null) {
            map.put("nutritionFacts", nutritionFacts.toFirestoreMap());
        }
        
        if (ratings != null) {
            map.put("ratings", ratings.toFirestoreMap());
        }
        
        return map;
    }
    
    public static Recipe fromFirestoreMap(Map<String, Object> data) {
        Recipe recipe = new Recipe();
        recipe.setId((String) data.get("id"));
        recipe.setTitle((String) data.get("title"));
        recipe.setDescription((String) data.get("description"));
        recipe.setImageUrl((String) data.get("imageUrl"));
        recipe.setCreatedBy((String) data.get("createdBy"));
        recipe.setAIGenerated((Boolean) data.getOrDefault("isAIGenerated", false));
        recipe.setDifficulty((String) data.get("difficulty"));
        recipe.setPrepTime((Integer) data.get("prepTime"));
        recipe.setCookTime((Integer) data.get("cookTime"));
        recipe.setServings((Integer) data.get("servings"));
        recipe.setCuisineType((String) data.get("cuisineType"));
        recipe.setMealType((String) data.get("mealType"));
        recipe.setPublic((Boolean) data.getOrDefault("isPublic", false));
        recipe.setViewCount((Integer) data.getOrDefault("viewCount", 0));
        recipe.setSaveCount((Integer) data.getOrDefault("saveCount", 0));
        recipe.setMadeCount((Integer) data.getOrDefault("madeCount", 0));
        recipe.setCreatedAt(BaseEntity.timestampToLocalDateTime(data.get("createdAt")));
        recipe.setUpdatedAt(BaseEntity.timestampToLocalDateTime(data.get("updatedAt")));
        
        if (data.get("tags") instanceof List) {
            recipe.setTags((List<String>) data.get("tags"));
        }
        
        if (data.get("ingredients") instanceof List) {
            List<Map<String, Object>> ingredientData = (List<Map<String, Object>>) data.get("ingredients");
            List<RecipeIngredient> ingredients = new ArrayList<>();
            for (Map<String, Object> ingredientMap : ingredientData) {
                ingredients.add(RecipeIngredient.fromFirestoreMap(ingredientMap));
            }
            recipe.setIngredients(ingredients);
        }
        
        if (data.get("instructions") instanceof List) {
            List<Map<String, Object>> instructionData = (List<Map<String, Object>>) data.get("instructions");
            List<RecipeInstruction> instructions = new ArrayList<>();
            for (Map<String, Object> instructionMap : instructionData) {
                instructions.add(RecipeInstruction.fromFirestoreMap(instructionMap));
            }
            recipe.setInstructions(instructions);
        }
        
        if (data.get("nutritionFacts") instanceof Map) {
            recipe.setNutritionFacts(NutritionFacts.fromFirestoreMap((Map<String, Object>) data.get("nutritionFacts")));
        }
        
        if (data.get("ratings") instanceof Map) {
            recipe.setRatings(RecipeRatings.fromFirestoreMap((Map<String, Object>) data.get("ratings")));
        }
        
        return recipe;
    }
    
    public Integer getTotalTime() {
        int total = 0;
        if (prepTime != null) total += prepTime;
        if (cookTime != null) total += cookTime;
        return total;
    }
    
    public void incrementViewCount() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0) + 1;
        updateTimestamp();
    }
    
    public void incrementSaveCount() {
        this.saveCount = (this.saveCount != null ? this.saveCount : 0) + 1;
        updateTimestamp();
    }
    
    public void incrementMadeCount() {
        this.madeCount = (this.madeCount != null ? this.madeCount : 0) + 1;
        updateTimestamp();
    }
    
    public void addRating(double rating) {
        if (this.ratings == null) {
            this.ratings = new RecipeRatings();
        }
        this.ratings.addRating(rating);
        updateTimestamp();
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public boolean isAIGenerated() { return isAIGenerated; }
    public void setAIGenerated(boolean AIGenerated) { isAIGenerated = AIGenerated; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public Integer getPrepTime() { return prepTime; }
    public void setPrepTime(Integer prepTime) { this.prepTime = prepTime; }
    
    public Integer getCookTime() { return cookTime; }
    public void setCookTime(Integer cookTime) { this.cookTime = cookTime; }
    
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredient> ingredients) { this.ingredients = ingredients; }
    
    public List<RecipeInstruction> getInstructions() { return instructions; }
    public void setInstructions(List<RecipeInstruction> instructions) { this.instructions = instructions; }
    
    public NutritionFacts getNutritionFacts() { return nutritionFacts; }
    public void setNutritionFacts(NutritionFacts nutritionFacts) { this.nutritionFacts = nutritionFacts; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public RecipeRatings getRatings() { return ratings; }
    public void setRatings(RecipeRatings ratings) { this.ratings = ratings; }
    
    public String getCuisineType() { return cuisineType; }
    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
    
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    
    public Integer getSaveCount() { return saveCount; }
    public void setSaveCount(Integer saveCount) { this.saveCount = saveCount; }
    
    public Integer getMadeCount() { return madeCount; }
    public void setMadeCount(Integer madeCount) { this.madeCount = madeCount; }
}