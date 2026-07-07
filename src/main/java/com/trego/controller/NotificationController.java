package com.trego.controller;

import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.NotificationFeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-app notification feed at {@code /social/notifications}
 * (→ {@code /api/social/notifications}). Flat {@code {success, ...}} bodies,
 * matching the Flutter {@code SocialService}.
 */
@RestController
@RequestMapping("/social/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationFeedService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            Map<String, Object> result = service.list(principal.getFirebaseUid(), limit);
            Map<String, Object> body = ok();
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to load notifications", e);
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            Map<String, Object> body = ok();
            body.put("unreadCount", service.unreadCount(principal.getFirebaseUid()));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to load unread count", e);
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.markRead(principal.getFirebaseUid(), id);
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            return fail("Failed to mark notification read", e);
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.markAllRead(principal.getFirebaseUid());
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            return fail("Failed to mark all read", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.delete(principal.getFirebaseUid(), id);
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            return fail("Failed to delete notification", e);
        }
    }

    // --- helpers ---

    private static boolean unauthenticated(FirebaseUserPrincipal principal) {
        return principal == null || principal.getUser() == null;
    }

    private static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        return m;
    }

    private static ResponseEntity<Map<String, Object>> unauthorized() {
        return status(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private ResponseEntity<Map<String, Object>> fail(String message, Exception e) {
        logger.error("{}: {}", message, e.getMessage());
        return status(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseEntity<Map<String, Object>> status(HttpStatus code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(code).body(body);
    }
}
