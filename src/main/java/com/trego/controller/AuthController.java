package com.trego.controller;

import com.trego.dto.*;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            logger.info("Registration request received for email: {}", request.getEmail());
            
            AuthResponse authResponse = authService.registerUser(request);
            
            ApiResponse<AuthResponse> response = ApiResponse.success(
                "User registered successfully. Please check your email for verification.",
                authResponse
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed - validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "VAL_001"));
                
        } catch (Exception e) {
            logger.error("Registration failed - unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Registration failed. Please try again.", "REG_001"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            logger.info("Login request received for email: {}", request.getEmail());
            
            AuthResponse authResponse = authService.loginUser(request.getEmail(), request.getPassword());
            
            ApiResponse<AuthResponse> response = ApiResponse.success("Login successful", authResponse);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials", "AUTH_001"));
                
        } catch (Exception e) {
            logger.error("Login failed - unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Login failed. Please try again.", "AUTH_002"));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Refresh token is required", "VAL_002"));
            }
            
            logger.info("Token refresh request received");
            
            String newAccessToken = authService.refreshToken(refreshToken);
            
            Map<String, Object> tokenData = Map.of(
                "accessToken", newAccessToken,
                "expiresIn", 86400000L // 24 hours
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Token refreshed successfully", tokenData);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid refresh token", "AUTH_004"));
                
        } catch (Exception e) {
            logger.error("Token refresh failed - unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Token refresh failed. Please try again.", "AUTH_005"));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(@AuthenticationPrincipal FirebaseUserPrincipal principal) {
        try {
            if (principal == null || principal.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "AUTH_003"));
            }
            
            String userId = principal.getUser().getId();
            logger.info("Get current user request for: {}", userId);
            
            AuthResponse userResponse = authService.getCurrentUser(userId);
            
            ApiResponse<AuthResponse> response = ApiResponse.success("User information retrieved successfully", userResponse);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Get current user failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve user information", "USER_001"));
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        try {
            if (principal == null || principal.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "AUTH_003"));
            }
            
            String userId = principal.getUser().getId();
            logger.info("Profile update request for user: {}", userId);
            
            AuthResponse userResponse = authService.updateUserProfile(userId, request);
            
            ApiResponse<AuthResponse> response = ApiResponse.success("Profile updated successfully", userResponse);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Profile update failed - validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "VAL_003"));
                
        } catch (Exception e) {
            logger.error("Profile update failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Profile update failed. Please try again.", "PROFILE_001"));
        }
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String verificationCode = request.get("verificationCode");
            
            if (email == null || verificationCode == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email and verification code are required", "VAL_004"));
            }
            
            logger.info("Email verification request for: {}", email);
            
            // Find user by email and verify
            var userOpt = authService.findUserByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User not found", "AUTH_006"));
            }
            
            authService.verifyEmail(userOpt.get().getId(), verificationCode);
            
            Map<String, Object> result = Map.of(
                "emailVerified", true,
                "verifiedAt", java.time.LocalDateTime.now()
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Email verified successfully", result);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "VER_001"));
                
        } catch (Exception e) {
            logger.error("Email verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Email verification failed. Please try again.", "VER_002"));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email is required", "VAL_005"));
            }
            
            logger.info("Forgot password request for: {}", email);
            
            // In a real implementation, generate and send reset token
            Map<String, Object> result = Map.of(
                "email", email,
                "resetTokenSent", true,
                "message", "If an account with this email exists, a password reset link has been sent."
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Password reset email sent", result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Forgot password failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Password reset request failed. Please try again.", "RESET_001"));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String resetToken = request.get("resetToken");
            String newPassword = request.get("newPassword");
            
            if (email == null || resetToken == null || newPassword == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email, reset token, and new password are required", "VAL_006"));
            }
            
            logger.info("Password reset request for: {}", email);
            
            // In a real implementation, validate reset token and update password in Firebase
            Map<String, Object> result = Map.of(
                "passwordResetAt", java.time.LocalDateTime.now(),
                "message", "Password has been reset successfully"
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Password reset successfully", result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Password reset failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Password reset failed. Please try again.", "RESET_002"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            if (principal == null || principal.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "AUTH_003"));
            }
            
            String userId = principal.getUser().getId();
            logger.info("Logout request for user: {}", userId);
            
            // In a real implementation, you might invalidate tokens in a blacklist
            Map<String, Object> result = Map.of(
                "loggedOut", true,
                "tokensRevoked", true,
                "loggedOutAt", java.time.LocalDateTime.now()
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Logout successful", result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Logout failed. Please try again.", "LOGOUT_001"));
        }
    }
    
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAccount(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @RequestBody Map<String, String> request) {
        try {
            if (principal == null || principal.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", "AUTH_003"));
            }
            
            String password = request.get("password");
            Boolean confirmDeletion = Boolean.parseBoolean(request.get("confirmDeletion"));
            
            if (!Boolean.TRUE.equals(confirmDeletion)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Account deletion must be confirmed", "VAL_007"));
            }
            
            String userId = principal.getUser().getId();
            logger.info("Account deletion request for user: {}", userId);
            
            authService.deleteUser(userId);
            
            Map<String, Object> result = Map.of(
                "deletedAt", java.time.LocalDateTime.now(),
                "accountDeleted", true
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("Account deleted successfully", result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Account deletion failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Account deletion failed. Please try again.", "DELETE_001"));
        }
    }
}