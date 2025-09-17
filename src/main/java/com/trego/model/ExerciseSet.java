package com.trego.model;

import java.util.HashMap;
import java.util.Map;

public class ExerciseSet {
    
    private Integer setNumber;
    private Integer reps;
    private Double weight;
    private Integer duration;
    private Double distance;
    private Integer restTime;
    private Integer rpe;
    
    public ExerciseSet() {}
    
    public ExerciseSet(Integer setNumber, Integer reps, Double weight) {
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }
    
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("setNumber", setNumber);
        map.put("reps", reps);
        map.put("weight", weight);
        map.put("duration", duration);
        map.put("distance", distance);
        map.put("restTime", restTime);
        map.put("rpe", rpe);
        return map;
    }
    
    public static ExerciseSet fromFirestoreMap(Map<String, Object> data) {
        ExerciseSet set = new ExerciseSet();
        set.setSetNumber((Integer) data.get("setNumber"));
        set.setReps((Integer) data.get("reps"));
        set.setDuration((Integer) data.get("duration"));
        set.setRestTime((Integer) data.get("restTime"));
        set.setRpe((Integer) data.get("rpe"));
        
        if (data.get("weight") != null) {
            set.setWeight(((Number) data.get("weight")).doubleValue());
        }
        
        if (data.get("distance") != null) {
            set.setDistance(((Number) data.get("distance")).doubleValue());
        }
        
        return set;
    }
    
    // Getters and Setters
    public Integer getSetNumber() { return setNumber; }
    public void setSetNumber(Integer setNumber) { this.setNumber = setNumber; }
    
    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }
    
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    
    public Integer getRestTime() { return restTime; }
    public void setRestTime(Integer restTime) { this.restTime = restTime; }
    
    public Integer getRpe() { return rpe; }
    public void setRpe(Integer rpe) { this.rpe = rpe; }
}