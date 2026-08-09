package com.trego.controller;

import com.trego.model.WorkoutSession;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.ActivitySessionService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Manually-logged multi-activity sessions at {@code /workouts} (→ {@code /api/workouts}). */
@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    private static final Logger logger = LoggerFactory.getLogger(WorkoutController.class);

    @Autowired private ActivitySessionService service;

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> log(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauth(principal)) return status(HttpStatus.UNAUTHORIZED, "Authentication required");
        try {
            WorkoutSession s = service.logSession(principal.getFirebaseUid(), body);
            Map<String, Object> m = ok();
            m.put("session", s.toFirestoreMap());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            logger.error("Log session failed: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST, "Failed to log session");
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> history(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauth(principal)) return status(HttpStatus.UNAUTHORIZED, "Authentication required");
        try {
            List<Map<String, Object>> out = new ArrayList<>();
            for (WorkoutSession s : service.getHistory(principal.getFirebaseUid())) out.add(s.toFirestoreMap());
            Map<String, Object> m = ok(); m.put("sessions", out);
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            logger.error("History failed: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST, "Failed to load history");
        }
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Object>> detail(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauth(principal)) return status(HttpStatus.UNAUTHORIZED, "Authentication required");
        try {
            WorkoutSession s = service.getSession(principal.getFirebaseUid(), id);
            if (s == null) return status(HttpStatus.NOT_FOUND, "Session not found");
            Map<String, Object> m = ok(); m.put("session", s.toFirestoreMap());
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            logger.error("Detail failed: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST, "Failed to load session");
        }
    }

    @GetMapping("/prs")
    public ResponseEntity<Map<String, Object>> prs(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (unauth(principal)) return status(HttpStatus.UNAUTHORIZED, "Authentication required");
        try {
            Map<String, Object> m = ok(); m.put("prs", service.computePRs(principal.getFirebaseUid()));
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            logger.error("PRs failed: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST, "Failed to compute PRs");
        }
    }

    private static boolean unauth(FirebaseUserPrincipal p) { return p == null || p.getUser() == null; }
    private static Map<String, Object> ok() { Map<String, Object> m = new LinkedHashMap<>(); m.put("success", true); return m; }
    private static ResponseEntity<Map<String, Object>> status(HttpStatus st, String msg) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("success", false); m.put("message", msg);
        return ResponseEntity.status(st).body(m);
    }
}
