package com.trego.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class NutritionEntry extends BaseEntity {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("date")
    private LocalDate date;
    
    @JsonProperty("mealType")
    private String mealType; // breakfast, lunch, dinner, snack
    
    @JsonProperty("foodName")
    private String foodName;
    
    @JsonProperty("brand")
    private String brand;
    
    @JsonProperty("quantity")
    private Double quantity;
    
    @JsonProperty("unit")
    private String unit; // grams, ounces, cups, pieces
    
    @JsonProperty("calories")
    private Double calories;
    
    @JsonProperty("macros")
    private Map<String, Double> macros = new HashMap<>(); // protein, carbs, fat, fiber
    
    @JsonProperty("micronutrients")
    private Map<String, Double> micronutrients = new HashMap<>(); // vitamins, minerals
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonProperty("loggedAt")
    private LocalDateTime loggedAt;
    
    public NutritionEntry() {
        super();
        this.date = LocalDate.now();
        this.loggedAt = LocalDateTime.now();
        initializeMacros();
    }
    
    public NutritionEntry(String userId) {
        super();
        this.userId = userId;
        this.date = LocalDate.now();
        this.loggedAt = LocalDateTime.now();
        initializeMacros();
    }
    
    private void initializeMacros() {
        macros.put("protein", 0.0);
        macros.put("carbs", 0.0);
        macros.put("fat", 0.0);
        macros.put("fiber", 0.0);
        macros.put("sugar", 0.0);
        macros.put("sodium", 0.0);
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("userId", this.userId);
        map.put("date", this.date);
        map.put("mealType", this.mealType);
        map.put("foodName", this.foodName);
        map.put("brand", this.brand);
        map.put("quantity", this.quantity);
        map.put("unit", this.unit);
        map.put("calories", this.calories);
        map.put("macros", this.macros);
        map.put("micronutrients", this.micronutrients);
        map.put("notes", this.notes);
        map.put("loggedAt", this.loggedAt);
        return map;
    }
    
    public static NutritionEntry fromFirestoreMap(Map<String, Object> map) {
        NutritionEntry entry = new NutritionEntry();
        entry.setId((String) map.get("id"));
        entry.setUserId((String) map.get("userId"));
        entry.setMealType((String) map.get("mealType"));
        entry.setFoodName((String) map.get("foodName"));
        entry.setBrand((String) map.get("brand"));
        entry.setQuantity((Double) map.get("quantity"));
        entry.setUnit((String) map.get("unit"));
        entry.setCalories((Double) map.get("calories"));
        entry.setNotes((String) map.get("notes"));
        
        if (map.get("date") != null) {
            entry.setDate((LocalDate) map.get("date"));
        }
        if (map.get("loggedAt") != null) {
            entry.setLoggedAt((LocalDateTime) map.get("loggedAt"));
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Double> macrosMap = (Map<String, Double>) map.get("macros");
        if (macrosMap != null) {
            entry.setMacros(macrosMap);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Double> micronutrientsMap = (Map<String, Double>) map.get("micronutrients");
        if (micronutrientsMap != null) {
            entry.setMicronutrients(micronutrientsMap);
        }
        
        return entry;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public String getMealType() {
        return mealType;
    }
    
    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    
    public String getFoodName() {
        return foodName;
    }
    
    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public Double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public Double getCalories() {
        return calories;
    }
    
    public void setCalories(Double calories) {
        this.calories = calories;
    }
    
    public Map<String, Double> getMacros() {
        return macros;
    }
    
    public void setMacros(Map<String, Double> macros) {
        this.macros = macros;
    }
    
    public Map<String, Double> getMicronutrients() {
        return micronutrients;
    }
    
    public void setMicronutrients(Map<String, Double> micronutrients) {
        this.micronutrients = micronutrients;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
    
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
    
    public Double getProtein() {
        return macros.getOrDefault("protein", 0.0);
    }
    
    public Double getCarbs() {
        return macros.getOrDefault("carbs", 0.0);
    }
    
    public Double getFat() {
        return macros.getOrDefault("fat", 0.0);
    }
}