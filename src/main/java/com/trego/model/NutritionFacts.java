package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class NutritionFacts {
    
    private Double calories;
    private Double protein;
    private Double carbs;
    private Double fat;
    private Double fiber;
    private Double sugar;
    private Double sodium;
    private Double cholesterol;
    private Map<String, Double> vitamins;
    private Map<String, Double> minerals;
    
    public NutritionFacts() {
        this.vitamins = new HashMap<>();
        this.minerals = new HashMap<>();
    }
    
    public NutritionFacts(Double calories, Double protein, Double carbs, Double fat) {
        this();
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("calories", calories);
        map.put("protein", protein);
        map.put("carbs", carbs);
        map.put("fat", fat);
        map.put("fiber", fiber);
        map.put("sugar", sugar);
        map.put("sodium", sodium);
        map.put("cholesterol", cholesterol);
        map.put("vitamins", vitamins);
        map.put("minerals", minerals);
        return map;
    }
    
    public static NutritionFacts fromFirestoreMap(Map<String, Object> data) {
        NutritionFacts nutrition = new NutritionFacts();
        nutrition.setCalories(getDoubleFromData(data, "calories"));
        nutrition.setProtein(getDoubleFromData(data, "protein"));
        nutrition.setCarbs(getDoubleFromData(data, "carbs"));
        nutrition.setFat(getDoubleFromData(data, "fat"));
        nutrition.setFiber(getDoubleFromData(data, "fiber"));
        nutrition.setSugar(getDoubleFromData(data, "sugar"));
        nutrition.setSodium(getDoubleFromData(data, "sodium"));
        nutrition.setCholesterol(getDoubleFromData(data, "cholesterol"));
        
        if (data.get("vitamins") instanceof Map) {
            Map<String, Object> vitaminData = (Map<String, Object>) data.get("vitamins");
            Map<String, Double> vitamins = new HashMap<>();
            for (Map.Entry<String, Object> entry : vitaminData.entrySet()) {
                vitamins.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
            }
            nutrition.setVitamins(vitamins);
        }
        
        if (data.get("minerals") instanceof Map) {
            Map<String, Object> mineralData = (Map<String, Object>) data.get("minerals");
            Map<String, Double> minerals = new HashMap<>();
            for (Map.Entry<String, Object> entry : mineralData.entrySet()) {
                minerals.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
            }
            nutrition.setMinerals(minerals);
        }
        
        return nutrition;
    }
    
    private static Double getDoubleFromData(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    // Getters and Setters
    public Double getCalories() { return calories; }
    public void setCalories(Double calories) { this.calories = calories; }
    
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    
    public Double getCarbs() { return carbs; }
    public void setCarbs(Double carbs) { this.carbs = carbs; }
    
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
    
    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }
    
    public Double getSugar() { return sugar; }
    public void setSugar(Double sugar) { this.sugar = sugar; }
    
    public Double getSodium() { return sodium; }
    public void setSodium(Double sodium) { this.sodium = sodium; }
    
    public Double getCholesterol() { return cholesterol; }
    public void setCholesterol(Double cholesterol) { this.cholesterol = cholesterol; }
    
    public Map<String, Double> getVitamins() { return vitamins; }
    public void setVitamins(Map<String, Double> vitamins) { this.vitamins = vitamins; }
    
    public Map<String, Double> getMinerals() { return minerals; }
    public void setMinerals(Map<String, Double> minerals) { this.minerals = minerals; }
}