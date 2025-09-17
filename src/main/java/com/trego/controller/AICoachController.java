package com.trego.controller;

import com.trego.dto.ApiResponse;
import com.trego.service.AICoachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/ai-coach")
public class AICoachController {
    
    private static final Logger logger = LoggerFactory.getLogger(AICoachController.class);
    
    @Autowired
    private AICoachService aiCoachService;
    
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPersonalizedRecommendations(Principal principal) {
        try {
            logger.info("AI coach recommendations request from user: {}", principal.getName());
            
            Map<String, Object> recommendations = aiCoachService.getPersonalizedRecommendations(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("AI coach recommendations retrieved successfully", recommendations));
            
        } catch (IllegalArgumentException e) {
            logger.warn("AI coach recommendations access denied for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage(), "ACCESS_DENIED"));
        } catch (Exception e) {
            logger.error("AI coach recommendations failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get AI coach recommendations", "AI_COACH_REC_001"));
        }
    }
    
    @GetMapping("/progress-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgressAnalysis(Principal principal) {
        try {
            logger.info("Progress analysis request from user: {}", principal.getName());
            
            Map<String, Object> analysis = aiCoachService.getProgressAnalysis(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Progress analysis completed successfully", analysis));
            
        } catch (Exception e) {
            logger.error("Progress analysis failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to analyze progress", "PROGRESS_ANALYSIS_001"));
        }
    }
    
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<String>> chatWithAICoach(
            @RequestBody Map<String, Object> request,
            Principal principal) {
        
        try {
            String message = (String) request.get("message");
            logger.info("AI coach chat request from user: {}", principal.getName());
            
            String response = aiCoachService.chatWithAICoach(principal.getName(), message);
            
            return ResponseEntity.ok(ApiResponse.success("AI coach response generated", response));
            
        } catch (IllegalArgumentException e) {
            logger.warn("AI coach chat access denied for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage(), "ACCESS_DENIED"));
        } catch (Exception e) {
            logger.error("AI coach chat failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get AI coach response", "AI_COACH_CHAT_001"));
        }
    }
}