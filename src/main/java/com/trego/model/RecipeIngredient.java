package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class RecipeIngredient {
    
    private String ingredientId;
    private String name;
    private Double quantity;
    private String unit;
    private String notes;
    private boolean isOptional;
    
    public RecipeIngredient() {}
    
    public RecipeIngredient(String name, Double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.isOptional = false;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ingredientId", ingredientId);
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("notes", notes);
        map.put("isOptional", isOptional);
        return map;
    }
    
    public static RecipeIngredient fromFirestoreMap(Map<String, Object> data) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setIngredientId((String) data.get("ingredientId"));
        ingredient.setName((String) data.get("name"));
        ingredient.setQuantity(((Number) data.get("quantity")).doubleValue());
        ingredient.setUnit((String) data.get("unit"));
        ingredient.setNotes((String) data.get("notes"));
        ingredient.setOptional((Boolean) data.getOrDefault("isOptional", false));
        return ingredient;
    }
    
    // Getters and Setters
    public String getIngredientId() { return ingredientId; }
    public void setIngredientId(String ingredientId) { this.ingredientId = ingredientId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isOptional() { return isOptional; }
    public void setOptional(boolean optional) { isOptional = optional; }
}