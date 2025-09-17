package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class WeatherData {
    
    private String condition;
    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private String windDirection;
    private Double visibility;
    private String description;
    
    public WeatherData() {}
    
    public WeatherData(String condition, Double temperature) {
        this.condition = condition;
        this.temperature = temperature;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("condition", condition);
        map.put("temperature", temperature);
        map.put("humidity", humidity);
        map.put("windSpeed", windSpeed);
        map.put("windDirection", windDirection);
        map.put("visibility", visibility);
        map.put("description", description);
        return map;
    }
    
    public static WeatherData fromFirestoreMap(Map<String, Object> data) {
        WeatherData weather = new WeatherData();
        weather.setCondition((String) data.get("condition"));
        weather.setWindDirection((String) data.get("windDirection"));
        weather.setDescription((String) data.get("description"));
        
        if (data.get("temperature") != null) {
            weather.setTemperature(((Number) data.get("temperature")).doubleValue());
        }
        
        if (data.get("humidity") != null) {
            weather.setHumidity(((Number) data.get("humidity")).doubleValue());
        }
        
        if (data.get("windSpeed") != null) {
            weather.setWindSpeed(((Number) data.get("windSpeed")).doubleValue());
        }
        
        if (data.get("visibility") != null) {
            weather.setVisibility(((Number) data.get("visibility")).doubleValue());
        }
        
        return weather;
    }
    
    // Getters and Setters
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    
    public String getWindDirection() { return windDirection; }
    public void setWindDirection(String windDirection) { this.windDirection = windDirection; }
    
    public Double getVisibility() { return visibility; }
    public void setVisibility(Double visibility) { this.visibility = visibility; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}