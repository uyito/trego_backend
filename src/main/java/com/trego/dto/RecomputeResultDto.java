package com.trego.dto;

import java.time.Instant;

public class RecomputeResultDto {
    private Instant recomputedAt;
    private int runCount;
    private long durationMs;

    public RecomputeResultDto() {}
    public RecomputeResultDto(Instant recomputedAt, int runCount, long durationMs) {
        this.recomputedAt = recomputedAt;
        this.runCount = runCount;
        this.durationMs = durationMs;
    }

    public Instant getRecomputedAt() { return recomputedAt; }
    public void setRecomputedAt(Instant v) { this.recomputedAt = v; }
    public int getRunCount() { return runCount; }
    public void setRunCount(int v) { this.runCount = v; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long v) { this.durationMs = v; }
}
