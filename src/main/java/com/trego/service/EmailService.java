package com.trego.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@trego.com}")
    private String fromEmail;
    
    public void sendVerificationEmail(String toEmail, String firstName, String verificationCode) {
        logger.info("Sending verification email to: {}", toEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Trego - Verify Your Email");
            
            String htmlContent = buildVerificationEmailHtml(firstName, verificationCode);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        logger.info("Sending password reset email to: {}", toEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Trego - Password Reset Request");
            
            String htmlContent = buildPasswordResetEmailHtml(firstName, resetToken);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    public void sendWelcomeEmail(String toEmail, String firstName) {
        logger.info("Sending welcome email to: {}", toEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Trego - Your Fitness Journey Begins!");
            
            String htmlContent = buildWelcomeEmailHtml(firstName);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
            // Don't throw exception for welcome emails as they're not critical
        }
    }
    
    public void sendWorkoutReminderEmail(String toEmail, String firstName, String workoutName) {
        logger.info("Sending workout reminder email to: {}", toEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Trego - Time for Your Workout!");
            message.setText(String.format(
                "Hi %s,\n\n" +
                "This is a friendly reminder that it's time for your %s workout!\n\n" +
                "Remember, consistency is key to achieving your fitness goals.\n\n" +
                "Keep pushing forward!\n\n" +
                "Best regards,\n" +
                "The Trego Team",
                firstName, workoutName
            ));
            
            mailSender.send(message);
            logger.info("Workout reminder email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send workout reminder email to {}: {}", toEmail, e.getMessage());
            // Don't throw exception for reminder emails
        }
    }
    
    private String buildVerificationEmailHtml(String firstName, String verificationCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Verify Your Email - Trego</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .code { background-color: #e7f3ff; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Trego!</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>Thank you for joining Trego! We're excited to help you on your fitness journey.</p>
                        <p>To complete your registration, please verify your email address by using the verification code below:</p>
                        <div class="code">%s</div>
                        <p>This code will expire in 24 hours for security reasons.</p>
                        <p>If you didn't create a Trego account, you can safely ignore this email.</p>
                        <p>Ready to transform your fitness routine? Let's get started!</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2023 Trego. All rights reserved.</p>
                        <p>This email was sent to you because you signed up for a Trego account.</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName, verificationCode);
    }
    
    public void sendEmail(String toEmail, String subject, String body) {
        logger.info("Sending generic email to: {} with subject: {}", toEmail, subject);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private String buildPasswordResetEmailHtml(String firstName, String resetToken) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Password Reset - Trego</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF6B6B; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; background-color: #4CAF50; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>We received a request to reset your Trego account password.</p>
                        <p>Your password reset token is:</p>
                        <p><strong>%s</strong></p>
                        <p>This token will expire in 1 hour for security reasons.</p>
                        <p>If you didn't request a password reset, you can safely ignore this email. Your password will remain unchanged.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2023 Trego. All rights reserved.</p>
                        <p>For security reasons, this link will expire in 1 hour.</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName, resetToken);
    }
    
    private String buildWelcomeEmailHtml(String firstName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Welcome to Trego!</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .feature { margin: 15px 0; padding: 10px; background-color: white; border-radius: 4px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Trego, %s!</h1>
                    </div>
                    <div class="content">
                        <p>Congratulations on taking the first step towards achieving your fitness goals!</p>
                        
                        <h3>What you can do with Trego:</h3>
                        <div class="feature">
                            <strong>🏋️ Personalized Workouts:</strong> AI-generated workout plans tailored to your goals and fitness level.
                        </div>
                        <div class="feature">
                            <strong>🥗 Nutrition Tracking:</strong> Log your meals and get personalized nutrition recommendations.
                        </div>
                        <div class="feature">
                            <strong>📊 Progress Tracking:</strong> Monitor your fitness journey with detailed analytics.
                        </div>
                        <div class="feature">
                            <strong>👥 Social Features:</strong> Connect with friends and participate in challenges.
                        </div>
                        
                        <p>You have <strong>7 days of free premium access</strong> to explore all our features!</p>
                        
                        <p>Ready to start? Complete your profile to get personalized recommendations.</p>
                        
                        <p>Let's make your fitness goals a reality!</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2023 Trego. All rights reserved.</p>
                        <p>Get ready to transform your fitness journey!</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName);
    }
}