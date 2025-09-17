package com.trego.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutSession extends BaseEntity {
    
    private String userId;
    private String workoutPlanId;
    private String sessionType;
    private String sessionName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String status;
    private WorkoutLocation location;
    private List<ExerciseLog> exercises;
    private Double totalCaloriesBurned;
    private Double averageHeartRate;
    private Double maxHeartRate;
    private List<GPSPoint> gpsRoute;
    private String mood;
    private Integer perceivedExertion;
    private String notes;
    private String summary;
    private WeatherData weatherConditions;
    private boolean isCompleted;
    
    public WorkoutSession() {
        super();
        this.exercises = new ArrayList<>();
        this.gpsRoute = new ArrayList<>();
        this.status = "CREATED";
        this.isCompleted = false;
    }
    
    public WorkoutSession(String userId, String sessionType) {
        this();
        this.userId = userId;
        this.sessionType = sessionType;
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("userId", userId);
        map.put("workoutPlanId", workoutPlanId);
        map.put("sessionType", sessionType);
        map.put("sessionName", sessionName);
        map.put("startTime", startTime != null ? startTime.toString() : null);
        map.put("endTime", endTime != null ? endTime.toString() : null);
        map.put("duration", duration);
        map.put("status", status);
        map.put("totalCaloriesBurned", totalCaloriesBurned);
        map.put("averageHeartRate", averageHeartRate);
        map.put("maxHeartRate", maxHeartRate);
        map.put("mood", mood);
        map.put("perceivedExertion", perceivedExertion);
        map.put("notes", notes);
        map.put("summary", summary);
        map.put("isCompleted", isCompleted);
        
        if (location != null) {
            map.put("location", location.toFirestoreMap());
        }
        
        if (exercises != null && !exercises.isEmpty()) {
            List<Map<String, Object>> exerciseMaps = new ArrayList<>();
            for (ExerciseLog exercise : exercises) {
                exerciseMaps.add(exercise.toFirestoreMap());
            }
            map.put("exercises", exerciseMaps);
        }
        
        if (gpsRoute != null && !gpsRoute.isEmpty()) {
            List<Map<String, Object>> gpsPoints = new ArrayList<>();
            for (GPSPoint point : gpsRoute) {
                gpsPoints.add(point.toFirestoreMap());
            }
            map.put("gpsRoute", gpsPoints);
        }
        
        if (weatherConditions != null) {
            map.put("weatherConditions", weatherConditions.toFirestoreMap());
        }
        
        return map;
    }
    
    public static WorkoutSession fromFirestoreMap(Map<String, Object> data) {
        WorkoutSession session = new WorkoutSession();
        session.setId((String) data.get("id"));
        session.setUserId((String) data.get("userId"));
        session.setWorkoutPlanId((String) data.get("workoutPlanId"));
        session.setSessionType((String) data.get("sessionType"));
        session.setSessionName((String) data.get("sessionName"));
        session.setDuration((Integer) data.get("duration"));
        session.setStatus((String) data.getOrDefault("status", "CREATED"));
        session.setMood((String) data.get("mood"));
        session.setPerceivedExertion((Integer) data.get("perceivedExertion"));
        session.setNotes((String) data.get("notes"));
        session.setSummary((String) data.get("summary"));
        session.setCompleted((Boolean) data.getOrDefault("isCompleted", false));
        session.setCreatedAt(BaseEntity.timestampToLocalDateTime(data.get("createdAt")));
        session.setUpdatedAt(BaseEntity.timestampToLocalDateTime(data.get("updatedAt")));
        
        if (data.get("startTime") instanceof String) {
            session.setStartTime(LocalDateTime.parse((String) data.get("startTime")));
        }
        
        if (data.get("endTime") instanceof String) {
            session.setEndTime(LocalDateTime.parse((String) data.get("endTime")));
        }
        
        if (data.get("totalCaloriesBurned") != null) {
            session.setTotalCaloriesBurned(((Number) data.get("totalCaloriesBurned")).doubleValue());
        }
        
        if (data.get("averageHeartRate") != null) {
            session.setAverageHeartRate(((Number) data.get("averageHeartRate")).doubleValue());
        }
        
        if (data.get("maxHeartRate") != null) {
            session.setMaxHeartRate(((Number) data.get("maxHeartRate")).doubleValue());
        }
        
        if (data.get("location") instanceof Map) {
            session.setLocation(WorkoutLocation.fromFirestoreMap((Map<String, Object>) data.get("location")));
        }
        
        if (data.get("exercises") instanceof List) {
            List<Map<String, Object>> exerciseData = (List<Map<String, Object>>) data.get("exercises");
            List<ExerciseLog> exercises = new ArrayList<>();
            for (Map<String, Object> exerciseMap : exerciseData) {
                exercises.add(ExerciseLog.fromFirestoreMap(exerciseMap));
            }
            session.setExercises(exercises);
        }
        
        if (data.get("gpsRoute") instanceof List) {
            List<Map<String, Object>> gpsData = (List<Map<String, Object>>) data.get("gpsRoute");
            List<GPSPoint> gpsRoute = new ArrayList<>();
            for (Map<String, Object> gpsMap : gpsData) {
                gpsRoute.add(GPSPoint.fromFirestoreMap(gpsMap));
            }
            session.setGpsRoute(gpsRoute);
        }
        
        if (data.get("weatherConditions") instanceof Map) {
            session.setWeatherConditions(WeatherData.fromFirestoreMap((Map<String, Object>) data.get("weatherConditions")));
        }
        
        return session;
    }
    
    public void startSession() {
        this.startTime = LocalDateTime.now();
        this.status = "IN_PROGRESS";
        updateTimestamp();
    }
    
    public void endSession() {
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.duration = (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
        this.status = "COMPLETED";
        this.isCompleted = true;
        updateTimestamp();
    }
    
    public void addGPSPoint(GPSPoint point) {
        if (this.gpsRoute == null) {
            this.gpsRoute = new ArrayList<>();
        }
        this.gpsRoute.add(point);
        updateTimestamp();
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getWorkoutPlanId() { return workoutPlanId; }
    public void setWorkoutPlanId(String workoutPlanId) { this.workoutPlanId = workoutPlanId; }
    
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public WorkoutLocation getLocation() { return location; }
    public void setLocation(WorkoutLocation location) { this.location = location; }
    
    public List<ExerciseLog> getExercises() { return exercises; }
    public void setExercises(List<ExerciseLog> exercises) { this.exercises = exercises; }
    
    public Double getTotalCaloriesBurned() { return totalCaloriesBurned; }
    public void setTotalCaloriesBurned(Double totalCaloriesBurned) { this.totalCaloriesBurned = totalCaloriesBurned; }
    
    public Double getAverageHeartRate() { return averageHeartRate; }
    public void setAverageHeartRate(Double averageHeartRate) { this.averageHeartRate = averageHeartRate; }
    
    public Double getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(Double maxHeartRate) { this.maxHeartRate = maxHeartRate; }
    
    public List<GPSPoint> getGpsRoute() { return gpsRoute; }
    public void setGpsRoute(List<GPSPoint> gpsRoute) { this.gpsRoute = gpsRoute; }
    
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    
    public Integer getPerceivedExertion() { return perceivedExertion; }
    public void setPerceivedExertion(Integer perceivedExertion) { this.perceivedExertion = perceivedExertion; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public WeatherData getWeatherConditions() { return weatherConditions; }
    public void setWeatherConditions(WeatherData weatherConditions) { this.weatherConditions = weatherConditions; }
    
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}