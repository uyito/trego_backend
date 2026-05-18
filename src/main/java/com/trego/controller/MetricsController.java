package com.trego.controller;

import com.trego.dto.MetricsSnapshotDto;
import com.trego.dto.RecomputeResultDto;
import com.trego.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MetricsController {

    @Autowired
    private MetricsService service;

    /** Read materialized metrics, recomputing if missing or stale. */
    @GetMapping("/me")
    public ResponseEntity<MetricsSnapshotDto> getMine(@AuthenticationPrincipal UserDetails principal) {
        String uid = principal.getUsername();
        return ResponseEntity.ok(service.getSnapshot(uid));
    }

    /** Trigger a recompute. Frontend calls this after a successful Run save. */
    @PostMapping("/me/recompute")
    public ResponseEntity<RecomputeResultDto> recompute(@AuthenticationPrincipal UserDetails principal) {
        String uid = principal.getUsername();
        return ResponseEntity.ok(service.recompute(uid));
    }
}
