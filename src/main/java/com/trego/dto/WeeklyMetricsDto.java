package com.trego.dto;

import java.time.Instant;

public class WeeklyMetricsDto {
    private String isoYearWeek;
    private Instant weekStart;
    private Instant weekEnd;
    private double totalKm;
    private int totalRuns;
    private long totalTimeMs;
    private long avgPaceSecPerKm;
    private double longestKm;
    private int streakDays;

    public WeeklyMetricsDto() {}

    public String getIsoYearWeek() { return isoYearWeek; }
    public void setIsoYearWeek(String v) { this.isoYearWeek = v; }
    public Instant getWeekStart() { return weekStart; }
    public void setWeekStart(Instant v) { this.weekStart = v; }
    public Instant getWeekEnd() { return weekEnd; }
    public void setWeekEnd(Instant v) { this.weekEnd = v; }
    public double getTotalKm() { return totalKm; }
    public void setTotalKm(double v) { this.totalKm = v; }
    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int v) { this.totalRuns = v; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(long v) { this.totalTimeMs = v; }
    public long getAvgPaceSecPerKm() { return avgPaceSecPerKm; }
    public void setAvgPaceSecPerKm(long v) { this.avgPaceSecPerKm = v; }
    public double getLongestKm() { return longestKm; }
    public void setLongestKm(double v) { this.longestKm = v; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int v) { this.streakDays = v; }
}
