package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class WorkoutLocation {
    
    private String type;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    
    public WorkoutLocation() {}
    
    public WorkoutLocation(String type, String name) {
        this.type = type;
        this.name = name;
    }
    
    public WorkoutLocation(String type, String name, Double latitude, Double longitude) {
        this(type, name);
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("name", name);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("address", address);
        return map;
    }
    
    public static WorkoutLocation fromFirestoreMap(Map<String, Object> data) {
        WorkoutLocation location = new WorkoutLocation();
        location.setType((String) data.get("type"));
        location.setName((String) data.get("name"));
        location.setAddress((String) data.get("address"));
        
        if (data.get("latitude") != null) {
            location.setLatitude(((Number) data.get("latitude")).doubleValue());
        }
        
        if (data.get("longitude") != null) {
            location.setLongitude(((Number) data.get("longitude")).doubleValue());
        }
        
        return location;
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}