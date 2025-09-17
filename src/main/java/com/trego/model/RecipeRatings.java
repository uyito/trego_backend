package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class RecipeRatings {
    
    private Double average;
    private Integer count;
    private Map<Integer, Integer> distribution;
    
    public RecipeRatings() {
        this.average = 0.0;
        this.count = 0;
        this.distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            this.distribution.put(i, 0);
        }
    }
    
    public void addRating(double rating) {
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }
        
        int ratingKey = (int) Math.round(rating);
        
        double totalRating = (this.average * this.count) + rating;
        this.count++;
        this.average = totalRating / this.count;
        
        this.distribution.put(ratingKey, this.distribution.get(ratingKey) + 1);
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("average", average);
        map.put("count", count);
        map.put("distribution", distribution);
        return map;
    }
    
    public static RecipeRatings fromFirestoreMap(Map<String, Object> data) {
        RecipeRatings ratings = new RecipeRatings();
        ratings.setAverage(((Number) data.getOrDefault("average", 0.0)).doubleValue());
        ratings.setCount((Integer) data.getOrDefault("count", 0));
        
        if (data.get("distribution") instanceof Map) {
            Map<String, Object> distMap = (Map<String, Object>) data.get("distribution");
            Map<Integer, Integer> distribution = new HashMap<>();
            for (Map.Entry<String, Object> entry : distMap.entrySet()) {
                distribution.put(Integer.parseInt(entry.getKey()), (Integer) entry.getValue());
            }
            ratings.setDistribution(distribution);
        }
        
        return ratings;
    }
    
    // Getters and Setters
    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }
    
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    
    public Map<Integer, Integer> getDistribution() { return distribution; }
    public void setDistribution(Map<Integer, Integer> distribution) { this.distribution = distribution; }
}