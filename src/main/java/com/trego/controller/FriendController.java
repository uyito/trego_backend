package com.trego.controller;

import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Friend-graph endpoints at {@code /social/friends} (→ {@code /api/social/friends}).
 * Flat {@code {success, ...}} response bodies, matching the Flutter
 * {@code SocialService}.
 */
@RestController
@RequestMapping("/social/friends")
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private FriendService service;

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> send(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String identifier = firstNonNull(req, "identifier", "email", "friendId");
            if (identifier == null || identifier.trim().isEmpty()) {
                return badRequest("An email is required");
            }
            Map<String, Object> result =
                    service.sendRequest(principal.getFirebaseUid(), identifier.trim(), str(req.get("message")));
            Map<String, Object> body = ok();
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return fail("Failed to send friend request", e);
        }
    }

    @PutMapping("/respond")
    public ResponseEntity<Map<String, Object>> respond(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            String requestId = str(req.get("requestId"));
            if (requestId == null) return badRequest("requestId is required");
            boolean accept = Boolean.TRUE.equals(req.get("accept"))
                    || "true".equalsIgnoreCase(str(req.get("accept")));
            service.respondToRequest(principal.getFirebaseUid(), requestId, accept);
            return ResponseEntity.ok(ok());
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (IllegalStateException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return fail("Failed to respond to request", e);
        }
    }

    @DeleteMapping("/request/{id}")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.cancelRequest(principal.getFirebaseUid(), id);
            return ResponseEntity.ok(ok());
        } catch (NoSuchElementException e) {
            return notFound(e.getMessage());
        } catch (SecurityException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return fail("Failed to cancel request", e);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> friends(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            List<Map<String, Object>> friends = service.getFriends(principal.getFirebaseUid());
            Map<String, Object> body = ok();
            body.put("friends", friends);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to load friends", e);
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> requests(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            List<Map<String, Object>> requests = service.getRequests(principal.getFirebaseUid());
            Map<String, Object> body = ok();
            body.put("requests", requests);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return fail("Failed to load friend requests", e);
        }
    }

    @DeleteMapping("/{friendUid}")
    public ResponseEntity<Map<String, Object>> unfriend(
            @PathVariable String friendUid,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauthenticated(principal)) return unauthorized();
        try {
            service.unfriend(principal.getFirebaseUid(), friendUid);
            return ResponseEntity.ok(ok());
        } catch (Exception e) {
            return fail("Failed to unfriend", e);
        }
    }

    // --- helpers ---

    private static boolean unauthenticated(FirebaseUserPrincipal principal) {
        return principal == null || principal.getUser() == null;
    }

    private static String firstNonNull(Map<String, Object> req, String... keys) {
        for (String k : keys) {
            String v = str(req.get(k));
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
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

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return status(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseEntity<Map<String, Object>> notFound(String message) {
        return status(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseEntity<Map<String, Object>> forbidden(String message) {
        return status(HttpStatus.FORBIDDEN, message);
    }

    private static ResponseEntity<Map<String, Object>> conflict(String message) {
        return status(HttpStatus.CONFLICT, message);
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
