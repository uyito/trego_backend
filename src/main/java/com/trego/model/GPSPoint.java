package com.trego.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class GPSPoint {
    
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Double accuracy;
    private LocalDateTime timestamp;
    
    public GPSPoint() {
        this.timestamp = LocalDateTime.now();
    }
    
    public GPSPoint(Double latitude, Double longitude) {
        this();
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public GPSPoint(Double latitude, Double longitude, Double altitude, Double speed) {
        this(latitude, longitude);
        this.altitude = altitude;
        this.speed = speed;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("altitude", altitude);
        map.put("speed", speed);
        map.put("accuracy", accuracy);
        map.put("timestamp", timestamp != null ? timestamp.toString() : null);
        return map;
    }
    
    public static GPSPoint fromFirestoreMap(Map<String, Object> data) {
        GPSPoint point = new GPSPoint();
        point.setLatitude(((Number) data.get("latitude")).doubleValue());
        point.setLongitude(((Number) data.get("longitude")).doubleValue());
        
        if (data.get("altitude") != null) {
            point.setAltitude(((Number) data.get("altitude")).doubleValue());
        }
        
        if (data.get("speed") != null) {
            point.setSpeed(((Number) data.get("speed")).doubleValue());
        }
        
        if (data.get("accuracy") != null) {
            point.setAccuracy(((Number) data.get("accuracy")).doubleValue());
        }
        
        if (data.get("timestamp") instanceof String) {
            point.setTimestamp(LocalDateTime.parse((String) data.get("timestamp")));
        }
        
        return point;
    }
    
    public double distanceTo(GPSPoint other) {
        if (other == null) return 0.0;
        
        double earthRadius = 6371; // kilometers
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude)) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c * 1000; // return distance in meters
    }
    
    // Getters and Setters
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }
    
    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }
    
    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}