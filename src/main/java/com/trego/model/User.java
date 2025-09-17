package com.trego.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User extends BaseEntity {
    
    @JsonProperty("email")
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    
    @JsonProperty("firstName")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @JsonProperty("lastName")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("emailVerified")
    private boolean emailVerified = false;
    
    @JsonProperty("lastLogin")
    private LocalDateTime lastLogin;
    
    @JsonProperty("isActive")
    private boolean isActive = true;
    
    @JsonProperty("profileComplete")
    private boolean profileComplete = false;
    
    @JsonProperty("subscriptionStatus")
    private String subscriptionStatus = "FREE"; // FREE, PREMIUM, PREMIUM_PLUS
    
    @JsonProperty("subscriptionId")
    private String subscriptionId;
    
    @JsonProperty("trialEndDate")
    private LocalDateTime trialEndDate;
    
    @JsonProperty("roles")
    private List<String> roles = new ArrayList<>();
    
    public User() {
        super();
        this.roles.add("USER");
    }
    
    public User(String id) {
        super(id);
        this.roles.add("USER");
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("email", this.email);
        map.put("firstName", this.firstName);
        map.put("lastName", this.lastName);
        map.put("phoneNumber", this.phoneNumber);
        map.put("emailVerified", this.emailVerified);
        map.put("lastLogin", this.lastLogin);
        map.put("isActive", this.isActive);
        map.put("profileComplete", this.profileComplete);
        map.put("subscriptionStatus", this.subscriptionStatus);
        map.put("subscriptionId", this.subscriptionId);
        map.put("trialEndDate", this.trialEndDate);
        map.put("roles", this.roles);
        return map;
    }
    
    public static User fromFirestoreMap(Map<String, Object> map) {
        User user = new User();
        user.setId((String) map.get("id"));
        user.setEmail((String) map.get("email"));
        user.setFirstName((String) map.get("firstName"));
        user.setLastName((String) map.get("lastName"));
        user.setPhoneNumber((String) map.get("phoneNumber"));
        user.setEmailVerified((Boolean) map.getOrDefault("emailVerified", false));
        user.setActive((Boolean) map.getOrDefault("isActive", true));
        user.setProfileComplete((Boolean) map.getOrDefault("profileComplete", false));
        user.setSubscriptionStatus((String) map.getOrDefault("subscriptionStatus", "FREE"));
        user.setSubscriptionId((String) map.get("subscriptionId"));
        
        if (map.get("lastLogin") != null) {
            user.setLastLogin((LocalDateTime) map.get("lastLogin"));
        }
        if (map.get("trialEndDate") != null) {
            user.setTrialEndDate((LocalDateTime) map.get("trialEndDate"));
        }
        if (map.get("createdAt") != null) {
            user.setCreatedAt((LocalDateTime) map.get("createdAt"));
        }
        if (map.get("updatedAt") != null) {
            user.setUpdatedAt((LocalDateTime) map.get("updatedAt"));
        }
        
        @SuppressWarnings("unchecked")
        List<String> rolesList = (List<String>) map.get("roles");
        if (rolesList != null) {
            user.setRoles(rolesList);
        }
        
        return user;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isPremiumUser() {
        return "PREMIUM".equals(subscriptionStatus) || "PREMIUM_PLUS".equals(subscriptionStatus);
    }
    
    public boolean isTrialActive() {
        return trialEndDate != null && trialEndDate.isAfter(LocalDateTime.now());
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
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
    
    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public boolean isProfileComplete() {
        return profileComplete;
    }
    
    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }
    
    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }
    
    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
    
    public String getSubscriptionId() {
        return subscriptionId;
    }
    
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    
    public LocalDateTime getTrialEndDate() {
        return trialEndDate;
    }
    
    public void setTrialEndDate(LocalDateTime trialEndDate) {
        this.trialEndDate = trialEndDate;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}