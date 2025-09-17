package com.trego.service;

import com.trego.model.PantryItem;
import com.trego.model.User;
import com.trego.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    public void sendExpiryAlert(String userId, PantryItem item, String alertType) throws ExecutionException, InterruptedException {
        logger.info("Sending expiry alert to user: {} for item: {} - type: {}", userId, item.getName(), alertType);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for expiry alert: {}", userId);
            return;
        }
        
        User user = userOpt.get();
        String subject = getExpiryAlertSubject(item, alertType);
        String message = getExpiryAlertMessage(item, alertType, user.getFirstName());
        
        try {
            emailService.sendEmail(user.getEmail(), subject, message);
            logger.info("Expiry alert sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send expiry alert to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    public void sendWorkoutReminder(String userId, String workoutType) throws ExecutionException, InterruptedException {
        logger.info("Sending workout reminder to user: {} for workout: {}", userId, workoutType);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for workout reminder: {}", userId);
            return;
        }
        
        User user = userOpt.get();
        String subject = "Time for your " + workoutType + " workout!";
        String message = String.format(
            "Hi %s,\n\nIt's time for your scheduled %s workout. Let's keep up the great work!\n\nBest regards,\nTrego Team",
            user.getFirstName(), workoutType
        );
        
        try {
            emailService.sendEmail(user.getEmail(), subject, message);
            logger.info("Workout reminder sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send workout reminder to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    public void sendMealPrepReminder(String userId) throws ExecutionException, InterruptedException {
        logger.info("Sending meal prep reminder to user: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for meal prep reminder: {}", userId);
            return;
        }
        
        User user = userOpt.get();
        String subject = "Time to prep your meals!";
        String message = String.format(
            "Hi %s,\n\nIt's meal prep time! Check out your personalized recipes and get cooking.\n\nHappy cooking!\nTrego Team",
            user.getFirstName()
        );
        
        try {
            emailService.sendEmail(user.getEmail(), subject, message);
            logger.info("Meal prep reminder sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send meal prep reminder to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    public void sendMotivationalMessage(String userId, String message) throws ExecutionException, InterruptedException {
        logger.info("Sending motivational message to user: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for motivational message: {}", userId);
            return;
        }
        
        User user = userOpt.get();
        String subject = "Stay motivated on your fitness journey!";
        String emailMessage = String.format(
            "Hi %s,\n\n%s\n\nKeep up the amazing work!\nTrego Team",
            user.getFirstName(), message
        );
        
        try {
            emailService.sendEmail(user.getEmail(), subject, emailMessage);
            logger.info("Motivational message sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send motivational message to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    private String getExpiryAlertSubject(PantryItem item, String alertType) {
        return switch (alertType) {
            case "expires_tomorrow" -> "⚠️ " + item.getName() + " expires tomorrow!";
            case "expires_soon" -> "📅 " + item.getName() + " expires in " + item.getDaysUntilExpiry() + " days";
            case "expired" -> "🚨 " + item.getName() + " has expired";
            default -> "Pantry Alert: " + item.getName();
        };
    }
    
    private String getExpiryAlertMessage(PantryItem item, String alertType, String firstName) {
        String baseMessage = String.format("Hi %s,\n\n", firstName);
        
        String alertMessage = switch (alertType) {
            case "expires_tomorrow" -> String.format(
                "Your %s is expiring tomorrow (%s). Consider using it in a recipe today!",
                item.getName(), item.getExpiryDate()
            );
            case "expires_soon" -> String.format(
                "Your %s will expire in %d days (%s). Plan to use it soon to avoid waste!",
                item.getName(), item.getDaysUntilExpiry(), item.getExpiryDate()
            );
            case "expired" -> String.format(
                "Your %s expired on %s. Please check and remove it from your pantry for safety.",
                item.getName(), item.getExpiryDate()
            );
            default -> String.format(
                "Please check your %s in your pantry.",
                item.getName()
            );
        };
        
        return baseMessage + alertMessage + "\n\nBest regards,\nTrego Team";
    }
}