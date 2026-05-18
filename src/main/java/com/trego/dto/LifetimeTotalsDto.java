package com.trego.dto;

public class LifetimeTotalsDto {
    private double totalKm;
    private int totalRuns;
    private long totalTimeMs;

    public LifetimeTotalsDto() {}

    public double getTotalKm() { return totalKm; }
    public void setTotalKm(double v) { this.totalKm = v; }
    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int v) { this.totalRuns = v; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(long v) { this.totalTimeMs = v; }
}
