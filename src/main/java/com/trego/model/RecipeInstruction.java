package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class RecipeInstruction {
    
    private Integer step;
    private String description;
    private Integer duration;
    private String imageUrl;
    private Integer temperature;
    
    public RecipeInstruction() {}
    
    public RecipeInstruction(Integer step, String description) {
        this.step = step;
        this.description = description;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("step", step);
        map.put("description", description);
        map.put("duration", duration);
        map.put("imageUrl", imageUrl);
        map.put("temperature", temperature);
        return map;
    }
    
    public static RecipeInstruction fromFirestoreMap(Map<String, Object> data) {
        RecipeInstruction instruction = new RecipeInstruction();
        instruction.setStep((Integer) data.get("step"));
        instruction.setDescription((String) data.get("description"));
        instruction.setDuration((Integer) data.get("duration"));
        instruction.setImageUrl((String) data.get("imageUrl"));
        instruction.setTemperature((Integer) data.get("temperature"));
        return instruction;
    }
    
    // Getters and Setters
    public Integer getStep() { return step; }
    public void setStep(Integer step) { this.step = step; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Integer getTemperature() { return temperature; }
    public void setTemperature(Integer temperature) { this.temperature = temperature; }
}