package com.trego.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseLog {
    
    private String exerciseId;
    private String name;
    private List<ExerciseSet> sets;
    private String notes;
    private Double totalWeight;
    private Integer totalReps;
    private Integer totalDuration;
    
    public ExerciseLog() {
        this.sets = new ArrayList<>();
    }
    
    public ExerciseLog(String exerciseId, String name) {
        this();
        this.exerciseId = exerciseId;
        this.name = name;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("exerciseId", exerciseId);
        map.put("name", name);
        map.put("notes", notes);
        map.put("totalWeight", totalWeight);
        map.put("totalReps", totalReps);
        map.put("totalDuration", totalDuration);
        
        if (sets != null && !sets.isEmpty()) {
            List<Map<String, Object>> setMaps = new ArrayList<>();
            for (ExerciseSet set : sets) {
                setMaps.add(set.toFirestoreMap());
            }
            map.put("sets", setMaps);
        }
        
        return map;
    }
    
    public static ExerciseLog fromFirestoreMap(Map<String, Object> data) {
        ExerciseLog log = new ExerciseLog();
        log.setExerciseId((String) data.get("exerciseId"));
        log.setName((String) data.get("name"));
        log.setNotes((String) data.get("notes"));
        
        if (data.get("totalWeight") != null) {
            log.setTotalWeight(((Number) data.get("totalWeight")).doubleValue());
        }
        
        log.setTotalReps((Integer) data.get("totalReps"));
        log.setTotalDuration((Integer) data.get("totalDuration"));
        
        if (data.get("sets") instanceof List) {
            List<Map<String, Object>> setData = (List<Map<String, Object>>) data.get("sets");
            List<ExerciseSet> sets = new ArrayList<>();
            for (Map<String, Object> setMap : setData) {
                sets.add(ExerciseSet.fromFirestoreMap(setMap));
            }
            log.setSets(sets);
        }
        
        return log;
    }
    
    public void addSet(ExerciseSet set) {
        if (this.sets == null) {
            this.sets = new ArrayList<>();
        }
        this.sets.add(set);
        calculateTotals();
    }
    
    private void calculateTotals() {
        if (sets == null || sets.isEmpty()) return;
        
        this.totalWeight = sets.stream()
            .filter(set -> set.getWeight() != null)
            .mapToDouble(ExerciseSet::getWeight)
            .sum();
            
        this.totalReps = sets.stream()
            .filter(set -> set.getReps() != null)
            .mapToInt(ExerciseSet::getReps)
            .sum();
            
        this.totalDuration = sets.stream()
            .filter(set -> set.getDuration() != null)
            .mapToInt(ExerciseSet::getDuration)
            .sum();
    }
    
    // Getters and Setters
    public String getExerciseId() { return exerciseId; }
    public void setExerciseId(String exerciseId) { this.exerciseId = exerciseId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<ExerciseSet> getSets() { return sets; }
    public void setSets(List<ExerciseSet> sets) { this.sets = sets; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Double getTotalWeight() { return totalWeight; }
    public void setTotalWeight(Double totalWeight) { this.totalWeight = totalWeight; }
    
    public Integer getTotalReps() { return totalReps; }
    public void setTotalReps(Integer totalReps) { this.totalReps = totalReps; }
    
    public Integer getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Integer totalDuration) { this.totalDuration = totalDuration; }
}