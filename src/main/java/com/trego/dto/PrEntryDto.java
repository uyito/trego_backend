package com.trego.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrEntryDto {
    private String runId;
    private double distanceKm;
    private Instant runStartTime;
    private Long paceSecPerKm;   // nullable; populated for fastest{1k,5k,10k}
    private Long durationMs;     // nullable; populated for longestDuration

    public PrEntryDto() {}

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public Instant getRunStartTime() { return runStartTime; }
    public void setRunStartTime(Instant runStartTime) { this.runStartTime = runStartTime; }
    public Long getPaceSecPerKm() { return paceSecPerKm; }
    public void setPaceSecPerKm(Long paceSecPerKm) { this.paceSecPerKm = paceSecPerKm; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
