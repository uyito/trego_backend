package com.trego.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ProfileUpdateRequest {
    
    @JsonProperty("firstName")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @JsonProperty("lastName")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @JsonProperty("phoneNumber")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
    private String phoneNumber;
    
    @JsonProperty("fitnessGoals")
    private List<String> fitnessGoals;
    
    @JsonProperty("activityLevel")
    @Pattern(regexp = "^(sedentary|low|moderate|high|very_high)$", 
             message = "Activity level must be one of: sedentary, low, moderate, high, very_high")
    private String activityLevel;
    
    @JsonProperty("height")
    @Min(value = 100, message = "Height must be at least 100 cm")
    @Max(value = 250, message = "Height must not exceed 250 cm")
    private Double height;
    
    @JsonProperty("weight")
    @Min(value = 30, message = "Weight must be at least 30 kg")
    @Max(value = 300, message = "Weight must not exceed 300 kg")
    private Double weight;
    
    @JsonProperty("targetWeight")
    @Min(value = 30, message = "Target weight must be at least 30 kg")
    @Max(value = 300, message = "Target weight must not exceed 300 kg")
    private Double targetWeight;
    
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;
    
    @JsonProperty("gender")
    @Pattern(regexp = "^(male|female|other)$", message = "Gender must be one of: male, female, other")
    private String gender;
    
    @JsonProperty("medicalConditions")
    private List<String> medicalConditions;
    
    @JsonProperty("dietaryRestrictions")
    private List<String> dietaryRestrictions;
    
    @JsonProperty("workoutFrequency")
    @Min(value = 1, message = "Workout frequency must be at least 1")
    @Max(value = 7, message = "Workout frequency must not exceed 7")
    private Integer workoutFrequency;
    
    @JsonProperty("preferredWorkoutTime")
    @Pattern(regexp = "^(morning|afternoon|evening)$", 
             message = "Preferred workout time must be one of: morning, afternoon, evening")
    private String preferredWorkoutTime;
    
    @JsonProperty("workoutDuration")
    @Min(value = 15, message = "Workout duration must be at least 15 minutes")
    @Max(value = 180, message = "Workout duration must not exceed 180 minutes")
    private Integer workoutDuration;
    
    @JsonProperty("experience")
    @Pattern(regexp = "^(beginner|intermediate|advanced)$", 
             message = "Experience must be one of: beginner, intermediate, advanced")
    private String experience;
    
    @JsonProperty("preferences")
    private Map<String, Object> preferences;
    
    public ProfileUpdateRequest() {}
    
    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
}