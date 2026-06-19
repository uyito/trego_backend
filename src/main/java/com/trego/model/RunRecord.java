package com.trego.model;

import java.time.Instant;

/**
 * Backend-side view of a single run document at users/{uid}/runs/{id}.
 * Decoupled from the existing Flutter Run model — only contains fields
 * MetricsService needs.
 */
public class RunRecord {
    private final String runId;
    private final double distanceKm;
    private final long durationMs;
    private final Instant startTime;

    public RunRecord(String runId, double distanceKm, long durationMs, Instant startTime) {
        this.runId = runId;
        this.distanceKm = distanceKm;
        this.durationMs = durationMs;
        this.startTime = startTime;
    }

    public String getRunId() { return runId; }
    public double getDistanceKm() { return distanceKm; }
    public long getDurationMs() { return durationMs; }
    public Instant getStartTime() { return startTime; }
}
