package com.trego.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.trego.dto.AuthResponse;
import com.trego.dto.ProfileUpdateRequest;
import com.trego.dto.RegisterRequest;
import com.trego.model.User;
import com.trego.model.UserProfile;
import com.trego.repository.UserProfileRepository;
import com.trego.repository.UserRepository;
import com.trego.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private FirebaseAuth firebaseAuth;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private EmailService emailService;
    
    public AuthResponse registerUser(RegisterRequest request) throws FirebaseAuthException, ExecutionException, InterruptedException {
        logger.info("Registering new user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.emailExists(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        
        try {
            // Create user in Firebase
            UserRecord.CreateRequest firebaseRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getFirstName() + " " + request.getLastName())
                    .setEmailVerified(false);
            
            UserRecord firebaseUser = firebaseAuth.createUser(firebaseRequest);
            logger.info("Firebase user created with UID: {}", firebaseUser.getUid());
            
            // Create user in our database
            User user = new User(firebaseUser.getUid());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setEmailVerified(false);
            user.setActive(true);
            user.setProfileComplete(false);
            
            // Set trial period for new users (7 days free trial)
            user.setTrialEndDate(LocalDateTime.now().plusDays(7));
            
            User savedUser = userRepository.save(user);
            logger.info("User saved to database with ID: {}", savedUser.getId());
            
            // Create default user profile
            createDefaultUserProfile(savedUser.getId());
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), "USER");
            String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());
            
            // Send verification email
            try {
                emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), generateVerificationCode(savedUser.getId()));
            } catch (Exception e) {
                logger.warn("Failed to send verification email to {}: {}", savedUser.getEmail(), e.getMessage());
            }
            
            AuthResponse response = AuthResponse.fromUserWithTokens(savedUser, accessToken, refreshToken, jwtUtil.getTokenExpirationTime());
            logger.info("User registration completed successfully for: {}", request.getEmail());
            
            return response;
            
        } catch (FirebaseAuthException e) {
            logger.error("Firebase error during user registration: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during user registration: {}", e.getMessage(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }
    
    public AuthResponse loginUser(String email, String password) throws ExecutionException, InterruptedException {
        logger.info("User login attempt for email: {}", email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.update(user);
        
        // Generate JWT tokens
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        AuthResponse response = AuthResponse.fromUserWithTokens(user, accessToken, refreshToken, jwtUtil.getTokenExpirationTime());
        
        // Include profile if it exists
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(user.getId());
            profileOpt.ifPresent(response::setProfile);
        } catch (Exception e) {
            logger.warn("Failed to load user profile for {}: {}", user.getId(), e.getMessage());
        }
        
        logger.info("User login successful for: {}", email);
        return response;
    }
    
    public Optional<User> syncFirebaseUser(FirebaseToken firebaseToken) throws ExecutionException, InterruptedException {
        logger.info("Syncing Firebase user: {}", firebaseToken.getUid());
        
        String uid = firebaseToken.getUid();
        String email = firebaseToken.getEmail();
        String name = firebaseToken.getName();
        
        Optional<User> existingUser = findUserById(uid);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            user.setEmailVerified(firebaseToken.isEmailVerified());
            userRepository.update(user);
            return Optional.of(user);
        }
        
        // Create new user from Firebase token
        User user = new User(uid);
        user.setEmail(email);
        user.setEmailVerified(firebaseToken.isEmailVerified());
        
        if (name != null && name.contains(" ")) {
            String[] nameParts = name.split(" ", 2);
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
        } else {
            user.setFirstName(name != null ? name : "User");
            user.setLastName("");
        }
        
        user.setActive(true);
        user.setLastLogin(LocalDateTime.now());
        user.setTrialEndDate(LocalDateTime.now().plusDays(7));
        
        User savedUser = userRepository.save(user);
        createDefaultUserProfile(savedUser.getId());
        
        logger.info("Firebase user synced and saved: {}", uid);
        return Optional.of(savedUser);
    }
    
    public String refreshToken(String refreshToken) throws ExecutionException, InterruptedException {
        logger.info("Refreshing access token");
        
        if (!jwtUtil.isRefreshToken(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        
        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        Optional<User> userOpt = findUserById(userId);
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), "USER");
        logger.info("Access token refreshed for user: {}", userId);
        
        return newAccessToken;
    }
    
    public AuthResponse getCurrentUser(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting current user: {}", userId);
        
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        AuthResponse response = AuthResponse.fromUser(user);
        
        // Include profile
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        profileOpt.ifPresent(response::setProfile);
        
        return response;
    }
    
    public AuthResponse updateUserProfile(String userId, ProfileUpdateRequest request) throws ExecutionException, InterruptedException {
        logger.info("Updating user profile: {}", userId);
        
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        
        // Update user basic info if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        userRepository.update(user);
        
        // Update user profile
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        UserProfile profile = profileOpt.orElse(new UserProfile(userId));
        
        updateProfileFromRequest(profile, request);
        userProfileRepository.save(profile);
        
        // Check if profile is complete
        boolean profileComplete = isProfileComplete(profile);
        if (profileComplete != user.isProfileComplete()) {
            user.setProfileComplete(profileComplete);
            userRepository.update(user);
        }
        
        AuthResponse response = AuthResponse.fromUser(user);
        response.setProfile(profile);
        
        logger.info("User profile updated successfully: {}", userId);
        return response;
    }
    
    public void verifyEmail(String userId, String verificationCode) throws ExecutionException, InterruptedException {
        logger.info("Verifying email for user: {}", userId);
        
        // In a real implementation, you would validate the verification code
        // For now, we'll just mark the email as verified
        
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        user.setEmailVerified(true);
        userRepository.update(user);
        
        logger.info("Email verified successfully for user: {}", userId);
    }
    
    public void deleteUser(String userId) throws ExecutionException, InterruptedException, FirebaseAuthException {
        logger.info("Deleting user: {}", userId);
        
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        // Delete from Firebase
        try {
            firebaseAuth.deleteUser(userId);
            logger.info("User deleted from Firebase: {}", userId);
        } catch (FirebaseAuthException e) {
            logger.warn("Failed to delete user from Firebase: {}", e.getMessage());
        }
        
        // Delete user profile
        userProfileRepository.deleteByUserId(userId);
        
        // Delete user
        userRepository.deleteById(userId);
        
        logger.info("User deleted successfully: {}", userId);
    }
    
    public Optional<User> findUserById(String userId) throws ExecutionException, InterruptedException {
        return userRepository.findById(userId);
    }
    
    public Optional<User> findUserByEmail(String email) throws ExecutionException, InterruptedException {
        return userRepository.findByEmail(email);
    }
    
    private void createDefaultUserProfile(String userId) throws ExecutionException, InterruptedException {
        UserProfile profile = new UserProfile(userId);
        userProfileRepository.save(profile);
        logger.info("Default user profile created for user: {}", userId);
    }
    
    private void updateProfileFromRequest(UserProfile profile, ProfileUpdateRequest request) {
        if (request.getFitnessGoals() != null) {
            profile.setFitnessGoals(request.getFitnessGoals());
        }
        if (request.getActivityLevel() != null) {
            profile.setActivityLevel(request.getActivityLevel());
        }
        if (request.getHeight() != null) {
            profile.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            profile.setWeight(request.getWeight());
        }
        if (request.getTargetWeight() != null) {
            profile.setTargetWeight(request.getTargetWeight());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getMedicalConditions() != null) {
            profile.setMedicalConditions(request.getMedicalConditions());
        }
        if (request.getDietaryRestrictions() != null) {
            profile.setDietaryRestrictions(request.getDietaryRestrictions());
        }
        if (request.getWorkoutFrequency() != null) {
            profile.setWorkoutFrequency(request.getWorkoutFrequency());
        }
        if (request.getPreferredWorkoutTime() != null) {
            profile.setPreferredWorkoutTime(request.getPreferredWorkoutTime());
        }
        if (request.getWorkoutDuration() != null) {
            profile.setWorkoutDuration(request.getWorkoutDuration());
        }
        if (request.getExperience() != null) {
            profile.setExperience(request.getExperience());
        }
        if (request.getPreferences() != null) {
            profile.setPreferences(request.getPreferences());
        }
        
        profile.updateTimestamp();
    }
    
    private boolean isProfileComplete(UserProfile profile) {
        return profile.getActivityLevel() != null &&
               profile.getHeight() != null &&
               profile.getWeight() != null &&
               profile.getDateOfBirth() != null &&
               profile.getGender() != null &&
               profile.getExperience() != null &&
               profile.getFitnessGoals() != null && !profile.getFitnessGoals().isEmpty();
    }
    
    private String generateVerificationCode(String userId) {
        // In a real implementation, you would generate a secure verification code
        // and store it in the database with an expiration time
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}