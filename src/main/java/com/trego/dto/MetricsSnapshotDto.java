package com.trego.dto;

import java.time.Instant;
import java.util.List;

public class MetricsSnapshotDto {
    private Instant computedAt;
    private WeeklyMetricsDto thisWeek;
    private PrsDto prs;
    private LifetimeTotalsDto totals;
    private List<WeeklyMetricsDto> history;

    public MetricsSnapshotDto() {}

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant v) { this.computedAt = v; }
    public WeeklyMetricsDto getThisWeek() { return thisWeek; }
    public void setThisWeek(WeeklyMetricsDto v) { this.thisWeek = v; }
    public PrsDto getPrs() { return prs; }
    public void setPrs(PrsDto v) { this.prs = v; }
    public LifetimeTotalsDto getTotals() { return totals; }
    public void setTotals(LifetimeTotalsDto v) { this.totals = v; }
    public List<WeeklyMetricsDto> getHistory() { return history; }
    public void setHistory(List<WeeklyMetricsDto> v) { this.history = v; }
}
