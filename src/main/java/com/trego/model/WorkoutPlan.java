package com.trego.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class WorkoutPlan extends BaseEntity {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("difficulty")
    private String difficulty; // beginner, intermediate, advanced
    
    @JsonProperty("duration")
    private Integer duration; // in minutes
    
    @JsonProperty("targetMuscleGroups")
    private List<String> targetMuscleGroups = new ArrayList<>();
    
    @JsonProperty("equipment")
    private List<String> equipment = new ArrayList<>();
    
    @JsonProperty("workoutType")
    private String workoutType; // cardio, strength, flexibility, hiit, etc.
    
    @JsonProperty("exercises")
    private List<Map<String, Object>> exercises = new ArrayList<>();
    
    @JsonProperty("caloriesBurnedEstimate")
    private Double caloriesBurnedEstimate;
    
    @JsonProperty("isAiGenerated")
    private boolean isAiGenerated = false;
    
    @JsonProperty("isPublic")
    private boolean isPublic = false;
    
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();
    
    @JsonProperty("completedCount")
    private Integer completedCount = 0;
    
    @JsonProperty("averageRating")
    private Double averageRating = 0.0;
    
    @JsonProperty("totalRatings")
    private Integer totalRatings = 0;
    
    public WorkoutPlan() {
        super();
    }
    
    public WorkoutPlan(String userId) {
        super();
        this.userId = userId;
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("userId", this.userId);
        map.put("name", this.name);
        map.put("description", this.description);
        map.put("difficulty", this.difficulty);
        map.put("duration", this.duration);
        map.put("targetMuscleGroups", this.targetMuscleGroups);
        map.put("equipment", this.equipment);
        map.put("workoutType", this.workoutType);
        map.put("exercises", this.exercises);
        map.put("caloriesBurnedEstimate", this.caloriesBurnedEstimate);
        map.put("isAiGenerated", this.isAiGenerated);
        map.put("isPublic", this.isPublic);
        map.put("tags", this.tags);
        map.put("completedCount", this.completedCount);
        map.put("averageRating", this.averageRating);
        map.put("totalRatings", this.totalRatings);
        return map;
    }
    
    public static WorkoutPlan fromFirestoreMap(Map<String, Object> map) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setId((String) map.get("id"));
        plan.setUserId((String) map.get("userId"));
        plan.setName((String) map.get("name"));
        plan.setDescription((String) map.get("description"));
        plan.setDifficulty((String) map.get("difficulty"));
        plan.setDuration((Integer) map.get("duration"));
        plan.setWorkoutType((String) map.get("workoutType"));
        plan.setCaloriesBurnedEstimate((Double) map.get("caloriesBurnedEstimate"));
        plan.setAiGenerated((Boolean) map.getOrDefault("isAiGenerated", false));
        plan.setPublic((Boolean) map.getOrDefault("isPublic", false));
        plan.setCompletedCount((Integer) map.getOrDefault("completedCount", 0));
        plan.setAverageRating((Double) map.getOrDefault("averageRating", 0.0));
        plan.setTotalRatings((Integer) map.getOrDefault("totalRatings", 0));
        
        @SuppressWarnings("unchecked")
        List<String> muscleGroups = (List<String>) map.get("targetMuscleGroups");
        if (muscleGroups != null) {
            plan.setTargetMuscleGroups(muscleGroups);
        }
        
        @SuppressWarnings("unchecked")
        List<String> equipmentList = (List<String>) map.get("equipment");
        if (equipmentList != null) {
            plan.setEquipment(equipmentList);
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> exercisesList = (List<Map<String, Object>>) map.get("exercises");
        if (exercisesList != null) {
            plan.setExercises(exercisesList);
        }
        
        @SuppressWarnings("unchecked")
        List<String> tagsList = (List<String>) map.get("tags");
        if (tagsList != null) {
            plan.setTags(tagsList);
        }
        
        return plan;
    }
    
    public void addExercise(String name, Integer sets, Integer reps, Double weight, Integer duration, String notes) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("name", name);
        exercise.put("sets", sets);
        exercise.put("reps", reps);
        exercise.put("weight", weight);
        exercise.put("duration", duration);
        exercise.put("notes", notes);
        exercise.put("restTime", 60); // default rest time in seconds
        this.exercises.add(exercise);
    }
    
    public void updateRating(Double newRating) {
        if (newRating >= 1.0 && newRating <= 5.0) {
            double totalScore = averageRating * totalRatings;
            totalRatings++;
            totalScore += newRating;
            averageRating = totalScore / totalRatings;
            updateTimestamp();
        }
    }
    
    public void incrementCompletedCount() {
        completedCount++;
        updateTimestamp();
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public List<String> getTargetMuscleGroups() {
        return targetMuscleGroups;
    }
    
    public void setTargetMuscleGroups(List<String> targetMuscleGroups) {
        this.targetMuscleGroups = targetMuscleGroups;
    }
    
    public List<String> getEquipment() {
        return equipment;
    }
    
    public void setEquipment(List<String> equipment) {
        this.equipment = equipment;
    }
    
    public String getWorkoutType() {
        return workoutType;
    }
    
    public void setWorkoutType(String workoutType) {
        this.workoutType = workoutType;
    }
    
    public List<Map<String, Object>> getExercises() {
        return exercises;
    }
    
    public void setExercises(List<Map<String, Object>> exercises) {
        this.exercises = exercises;
    }
    
    public Double getCaloriesBurnedEstimate() {
        return caloriesBurnedEstimate;
    }
    
    public void setCaloriesBurnedEstimate(Double caloriesBurnedEstimate) {
        this.caloriesBurnedEstimate = caloriesBurnedEstimate;
    }
    
    public boolean isAiGenerated() {
        return isAiGenerated;
    }
    
    public void setAiGenerated(boolean aiGenerated) {
        isAiGenerated = aiGenerated;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Integer getCompletedCount() {
        return completedCount;
    }
    
    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }
    
    public Double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
    
    public Integer getTotalRatings() {
        return totalRatings;
    }
    
    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }
}