package com.trego.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.*;

public class UserProfile extends BaseEntity {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("fitnessGoals")
    private List<String> fitnessGoals = new ArrayList<>();
    
    @JsonProperty("activityLevel")
    private String activityLevel; // sedentary, low, moderate, high, very_high
    
    @JsonProperty("height")
    private Double height; // in cm
    
    @JsonProperty("weight")
    private Double weight; // in kg
    
    @JsonProperty("targetWeight")
    private Double targetWeight;
    
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;
    
    @JsonProperty("gender")
    private String gender; // male, female, other
    
    @JsonProperty("medicalConditions")
    private List<String> medicalConditions = new ArrayList<>();
    
    @JsonProperty("dietaryRestrictions")
    private List<String> dietaryRestrictions = new ArrayList<>();
    
    @JsonProperty("workoutFrequency")
    private Integer workoutFrequency = 3; // times per week
    
    @JsonProperty("preferredWorkoutTime")
    private String preferredWorkoutTime; // morning, afternoon, evening
    
    @JsonProperty("workoutDuration")
    private Integer workoutDuration = 60; // in minutes
    
    @JsonProperty("experience")
    private String experience; // beginner, intermediate, advanced
    
    @JsonProperty("preferences")
    private Map<String, Object> preferences = new HashMap<>();
    
    @JsonProperty("stats")
    private Map<String, Object> stats = new HashMap<>();
    
    @JsonProperty("achievements")
    private List<Map<String, Object>> achievements = new ArrayList<>();
    
    public UserProfile() {
        super();
        initializeDefaults();
    }
    
    public UserProfile(String userId) {
        super();
        this.userId = userId;
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Default preferences
        preferences.put("units", "metric");
        preferences.put("timezone", "UTC");
        preferences.put("notifications", Map.of(
            "workouts", true,
            "nutrition", true,
            "social", false,
            "reminders", true
        ));
        preferences.put("privacy", Map.of(
            "shareWorkouts", true,
            "shareProgress", false,
            "shareNutrition", false
        ));
        
        // Default stats
        stats.put("totalWorkouts", 0);
        stats.put("totalCaloriesBurned", 0);
        stats.put("averageWorkoutDuration", 0);
        stats.put("currentStreak", 0);
        stats.put("longestStreak", 0);
        stats.put("weeklyGoalProgress", 0);
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("userId", this.userId);
        map.put("fitnessGoals", this.fitnessGoals);
        map.put("activityLevel", this.activityLevel);
        map.put("height", this.height);
        map.put("weight", this.weight);
        map.put("targetWeight", this.targetWeight);
        map.put("dateOfBirth", this.dateOfBirth);
        map.put("gender", this.gender);
        map.put("medicalConditions", this.medicalConditions);
        map.put("dietaryRestrictions", this.dietaryRestrictions);
        map.put("workoutFrequency", this.workoutFrequency);
        map.put("preferredWorkoutTime", this.preferredWorkoutTime);
        map.put("workoutDuration", this.workoutDuration);
        map.put("experience", this.experience);
        map.put("preferences", this.preferences);
        map.put("stats", this.stats);
        map.put("achievements", this.achievements);
        return map;
    }
    
    public static UserProfile fromFirestoreMap(Map<String, Object> map) {
        UserProfile profile = new UserProfile();
        profile.setId((String) map.get("id"));
        profile.setUserId((String) map.get("userId"));
        profile.setActivityLevel((String) map.get("activityLevel"));
        profile.setHeight((Double) map.get("height"));
        profile.setWeight((Double) map.get("weight"));
        profile.setTargetWeight((Double) map.get("targetWeight"));
        profile.setGender((String) map.get("gender"));
        profile.setWorkoutFrequency((Integer) map.get("workoutFrequency"));
        profile.setPreferredWorkoutTime((String) map.get("preferredWorkoutTime"));
        profile.setWorkoutDuration((Integer) map.get("workoutDuration"));
        profile.setExperience((String) map.get("experience"));
        
        if (map.get("dateOfBirth") != null) {
            profile.setDateOfBirth((LocalDate) map.get("dateOfBirth"));
        }
        
        @SuppressWarnings("unchecked")
        List<String> goalsList = (List<String>) map.get("fitnessGoals");
        if (goalsList != null) {
            profile.setFitnessGoals(goalsList);
        }
        
        @SuppressWarnings("unchecked")
        List<String> conditionsList = (List<String>) map.get("medicalConditions");
        if (conditionsList != null) {
            profile.setMedicalConditions(conditionsList);
        }
        
        @SuppressWarnings("unchecked")
        List<String> dietaryList = (List<String>) map.get("dietaryRestrictions");
        if (dietaryList != null) {
            profile.setDietaryRestrictions(dietaryList);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> prefsMap = (Map<String, Object>) map.get("preferences");
        if (prefsMap != null) {
            profile.setPreferences(prefsMap);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> statsMap = (Map<String, Object>) map.get("stats");
        if (statsMap != null) {
            profile.setStats(statsMap);
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> achievementsList = (List<Map<String, Object>>) map.get("achievements");
        if (achievementsList != null) {
            profile.setAchievements(achievementsList);
        }
        
        return profile;
    }
    
    public Double calculateBMI() {
        if (height != null && weight != null && height > 0 && weight > 0) {
            double heightInMeters = height / 100.0;
            return weight / (heightInMeters * heightInMeters);
        }
        return null;
    }
    
    public Integer calculateAge() {
        if (dateOfBirth != null) {
            return LocalDate.now().getYear() - dateOfBirth.getYear();
        }
        return null;
    }
    
    public Double calculateBMR() {
        if (weight != null && height != null && dateOfBirth != null && gender != null) {
            Integer age = calculateAge();
            if (age != null) {
                // Mifflin-St Jeor Equation
                if ("male".equalsIgnoreCase(gender)) {
                    return (10 * weight) + (6.25 * height) - (5 * age) + 5;
                } else if ("female".equalsIgnoreCase(gender)) {
                    return (10 * weight) + (6.25 * height) - (5 * age) - 161;
                }
            }
        }
        return null;
    }
    
    public Double calculateTDEE() {
        Double bmr = calculateBMR();
        if (bmr != null && activityLevel != null) {
            double multiplier = switch (activityLevel.toLowerCase()) {
                case "sedentary" -> 1.2;
                case "low" -> 1.375;
                case "moderate" -> 1.55;
                case "high" -> 1.725;
                case "very_high" -> 1.9;
                default -> 1.2;
            };
            return bmr * multiplier;
        }
        return null;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public List<String> getFitnessGoals() {
        return fitnessGoals;
    }
    
    public void setFitnessGoals(List<String> fitnessGoals) {
        this.fitnessGoals = fitnessGoals;
    }
    
    public String getActivityLevel() {
        return activityLevel;
    }
    
    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }
    
    public Double getHeight() {
        return height;
    }
    
    public void setHeight(Double height) {
        this.height = height;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public Double getTargetWeight() {
        return targetWeight;
    }
    
    public void setTargetWeight(Double targetWeight) {
        this.targetWeight = targetWeight;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public List<String> getMedicalConditions() {
        return medicalConditions;
    }
    
    public void setMedicalConditions(List<String> medicalConditions) {
        this.medicalConditions = medicalConditions;
    }
    
    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }
    
    public void setDietaryRestrictions(List<String> dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }
    
    public Integer getWorkoutFrequency() {
        return workoutFrequency;
    }
    
    public void setWorkoutFrequency(Integer workoutFrequency) {
        this.workoutFrequency = workoutFrequency;
    }
    
    public String getPreferredWorkoutTime() {
        return preferredWorkoutTime;
    }
    
    public void setPreferredWorkoutTime(String preferredWorkoutTime) {
        this.preferredWorkoutTime = preferredWorkoutTime;
    }
    
    public Integer getWorkoutDuration() {
        return workoutDuration;
    }
    
    public void setWorkoutDuration(Integer workoutDuration) {
        this.workoutDuration = workoutDuration;
    }
    
    public String getExperience() {
        return experience;
    }
    
    public void setExperience(String experience) {
        this.experience = experience;
    }
    
    public Map<String, Object> getPreferences() {
        return preferences;
    }
    
    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }
    
    public Map<String, Object> getStats() {
        return stats;
    }
    
    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }
    
    public List<Map<String, Object>> getAchievements() {
        return achievements;
    }
    
    public void setAchievements(List<Map<String, Object>> achievements) {
        this.achievements = achievements;
    }
    
    public List<String> getAllergies() {
        return dietaryRestrictions;
    }
    
    public String getCookingSkillLevel() {
        return experience;
    }
    
    public Double getTDEE() {
        return calculateTDEE();
    }
    
    public Double getCurrentWeight() {
        return weight;
    }
    
    public List<String> getCuisinePreferences() {
        Object cuisines = preferences.get("cuisinePreferences");
        if (cuisines instanceof List) {
            return (List<String>) cuisines;
        }
        return new ArrayList<>();
    }
}