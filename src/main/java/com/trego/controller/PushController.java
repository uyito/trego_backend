package com.trego.controller;

import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.DeviceTokenService;
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
 * FCM device-token registration at {@code /push} (→ {@code /api/push}). Flat
 * {@code {success, ...}} bodies, matching the app's other endpoints.
 */
@RestController
@RequestMapping("/push")
public class PushController {

    private static final Logger logger = LoggerFactory.getLogger(PushController.class);

    @Autowired
    private DeviceTokenService service;

    @PostMapping("/tokens")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String token = str(req.get("token"));
            if (token == null || token.trim().isEmpty()) {
                return status(HttpStatus.BAD_REQUEST, "Token is required");
            }
            service.register(principal.getFirebaseUid(), token, str(req.get("platform")));
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            logger.error("Register device token failed: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST, "Failed to register device token");
        }
    }

    @DeleteMapping("/tokens/{token}")
    public ResponseEntity<Map<String, Object>> unregister(
            @PathVariable String token,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.unregister(token);
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            logger.error("Unregister device token failed: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST, "Failed to unregister device token");
        }
    }

    // --- helpers ---

    private static boolean unauthenticated(FirebaseUserPrincipal principal) {
        return principal == null || principal.getUser() == null;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        return m;
    }

    private static ResponseEntity<Map<String, Object>> unauthorized() {
        return status(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private static ResponseEntity<Map<String, Object>> status(HttpStatus code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(code).body(body);
    }
}
