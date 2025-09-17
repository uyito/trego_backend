package com.trego.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trego.model.User;
import com.trego.model.UserProfile;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("emailVerified")
    private Boolean emailVerified;
    
    @JsonProperty("profileComplete")
    private Boolean profileComplete;
    
    @JsonProperty("subscriptionStatus")
    private String subscriptionStatus;
    
    @JsonProperty("accessToken")
    private String accessToken;
    
    @JsonProperty("refreshToken")
    private String refreshToken;
    
    @JsonProperty("expiresIn")
    private Long expiresIn;
    
    @JsonProperty("lastLogin")
    private LocalDateTime lastLogin;
    
    @JsonProperty("profile")
    private UserProfile profile;
    
    public AuthResponse() {}
    
    public AuthResponse(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.emailVerified = user.isEmailVerified();
        this.profileComplete = user.isProfileComplete();
        this.subscriptionStatus = user.getSubscriptionStatus();
        this.lastLogin = user.getLastLogin();
    }
    
    public static AuthResponse fromUser(User user) {
        return new AuthResponse(user);
    }
    
    public static AuthResponse fromUserWithTokens(User user, String accessToken, String refreshToken, Long expiresIn) {
        AuthResponse response = new AuthResponse(user);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        return response;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
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
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public Boolean getProfileComplete() {
        return profileComplete;
    }
    
    public void setProfileComplete(Boolean profileComplete) {
        this.profileComplete = profileComplete;
    }
    
    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }
    
    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public UserProfile getProfile() {
        return profile;
    }
    
    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}