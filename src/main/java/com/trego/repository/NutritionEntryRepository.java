package com.trego.repository;

import com.trego.model.NutritionEntry;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class NutritionEntryRepository extends FirestoreRepository<NutritionEntry> {
    
    public NutritionEntryRepository() {
        super("nutritionEntries", NutritionEntry::fromFirestoreMap);
    }
    
    public List<NutritionEntry> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return findByField("userId", userId);
    }
    
    public List<NutritionEntry> findByUserIdAndDate(String userId, LocalDate date) throws ExecutionException, InterruptedException {
        List<NutritionEntry> userEntries = findByUserId(userId);
        return userEntries.stream()
                .filter(entry -> entry.getDate().equals(date))
                .collect(Collectors.toList());
    }
    
    public List<NutritionEntry> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) throws ExecutionException, InterruptedException {
        List<NutritionEntry> userEntries = findByUserId(userId);
        return userEntries.stream()
                .filter(entry -> !entry.getDate().isBefore(startDate) && !entry.getDate().isAfter(endDate))
                .collect(Collectors.toList());
    }
    
    public void deleteAllByUserId(String userId) throws ExecutionException, InterruptedException {
        List<NutritionEntry> userEntries = findByUserId(userId);
        for (NutritionEntry entry : userEntries) {
            deleteById(entry.getId());
        }
    }
}