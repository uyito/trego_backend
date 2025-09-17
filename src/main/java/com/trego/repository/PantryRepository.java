package com.trego.repository;

import com.trego.model.PantryItem;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class PantryRepository extends FirestoreRepository<PantryItem> {
    
    public PantryRepository() {
        super("pantryItems", PantryItem::fromFirestoreMap);
    }
    
    public List<PantryItem> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return findByField("userId", userId);
    }
    
    public List<PantryItem> findByUserIdAndCategory(String userId, String category) throws ExecutionException, InterruptedException {
        List<PantryItem> userItems = findByUserId(userId);
        return userItems.stream()
                .filter(item -> category.equals(item.getCategory()))
                .collect(Collectors.toList());
    }
    
    public List<PantryItem> findExpiredItems(String userId) throws ExecutionException, InterruptedException {
        List<PantryItem> userItems = findByUserId(userId);
        return userItems.stream()
                .filter(item -> !item.isFinished())
                .filter(PantryItem::isExpired)
                .collect(Collectors.toList());
    }
    
    public List<PantryItem> findExpiringItems(String userId, int daysThreshold) throws ExecutionException, InterruptedException {
        List<PantryItem> userItems = findByUserId(userId);
        return userItems.stream()
                .filter(item -> !item.isFinished())
                .filter(item -> item.isNearExpiry(daysThreshold))
                .collect(Collectors.toList());
    }
    
    public List<PantryItem> findLowStockItems(String userId) throws ExecutionException, InterruptedException {
        List<PantryItem> userItems = findByUserId(userId);
        return userItems.stream()
                .filter(item -> !item.isFinished())
                .filter(PantryItem::isRunningLow)
                .collect(Collectors.toList());
    }
    
    public List<PantryItem> findByBarcode(String barcode) throws ExecutionException, InterruptedException {
        return findByField("barcode", barcode);
    }
    
    public List<PantryItem> findActiveItemsByUserId(String userId) throws ExecutionException, InterruptedException {
        List<PantryItem> userItems = findByUserId(userId);
        return userItems.stream()
                .filter(item -> !item.isFinished())
                .collect(Collectors.toList());
    }
    
    public void deleteAllByUserId(String userId) throws ExecutionException, InterruptedException {
        List<PantryItem> userItems = findByUserId(userId);
        for (PantryItem item : userItems) {
            deleteById(item.getId());
        }
    }
}