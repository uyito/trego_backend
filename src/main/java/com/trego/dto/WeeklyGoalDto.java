package com.trego.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * A user's weekly training goal. Both targets are optional (nullable) — a user
 * may set a distance target, a run-count target, both, or neither.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeeklyGoalDto {
    private Double targetKm;
    private Integer targetRuns;
    private Instant updatedAt;

    public WeeklyGoalDto() {}

    public WeeklyGoalDto(Double targetKm, Integer targetRuns, Instant updatedAt) {
        this.targetKm = targetKm;
        this.targetRuns = targetRuns;
        this.updatedAt = updatedAt;
    }

    public Double getTargetKm() { return targetKm; }
    public void setTargetKm(Double v) { this.targetKm = v; }
    public Integer getTargetRuns() { return targetRuns; }
    public void setTargetRuns(Integer v) { this.targetRuns = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
