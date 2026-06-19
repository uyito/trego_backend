package com.trego.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class PrsDto {
    private PrEntryDto fastest1k;
    private PrEntryDto fastest5k;
    private PrEntryDto fastest10k;
    private PrEntryDto longestDistance;
    private PrEntryDto longestDuration;

    public PrsDto() {}

    public PrEntryDto getFastest1k() { return fastest1k; }
    public void setFastest1k(PrEntryDto v) { this.fastest1k = v; }
    public PrEntryDto getFastest5k() { return fastest5k; }
    public void setFastest5k(PrEntryDto v) { this.fastest5k = v; }
    public PrEntryDto getFastest10k() { return fastest10k; }
    public void setFastest10k(PrEntryDto v) { this.fastest10k = v; }
    public PrEntryDto getLongestDistance() { return longestDistance; }
    public void setLongestDistance(PrEntryDto v) { this.longestDistance = v; }
    public PrEntryDto getLongestDuration() { return longestDuration; }
    public void setLongestDuration(PrEntryDto v) { this.longestDuration = v; }
}
