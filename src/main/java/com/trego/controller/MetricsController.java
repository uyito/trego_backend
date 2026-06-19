package com.trego.controller;

import com.trego.dto.MetricsSnapshotDto;
import com.trego.dto.RecomputeResultDto;
import com.trego.security.FirebaseUserPrincipal;
import com.trego.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    @Autowired
    private MetricsService service;

    /** Read materialized metrics, recomputing if missing or stale. */
    @GetMapping("/me")
    public ResponseEntity<MetricsSnapshotDto> getMine(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(service.getSnapshot(principal.getFirebaseUid()));
    }

    /** Trigger a recompute. Frontend calls this after a successful Run save. */
    @PostMapping("/me/recompute")
    public ResponseEntity<RecomputeResultDto> recompute(
            @AuthenticationPrincipal FirebaseUserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(service.recompute(principal.getFirebaseUid()));
    }
}
