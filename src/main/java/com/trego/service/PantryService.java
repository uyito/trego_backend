package com.trego.service;

import com.trego.model.PantryItem;
import com.trego.model.ProductInfo;
import com.trego.repository.PantryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class PantryService {
    
    private static final Logger logger = LoggerFactory.getLogger(PantryService.class);
    
    @Autowired
    private PantryRepository pantryRepository;
    
    @Autowired
    private BarcodeService barcodeService;
    
    @Autowired
    private NotificationService notificationService;
    
    public PantryItem addItem(String userId, PantryItem pantryItem) throws ExecutionException, InterruptedException {
        logger.info("Adding pantry item for user: {} - {}", userId, pantryItem.getName());
        
        pantryItem.setUserId(userId);
        
        // Enrich item with product information if barcode is provided
        if (pantryItem.getBarcode() != null && !pantryItem.getBarcode().isEmpty()) {
            try {
                ProductInfo productInfo = barcodeService.lookupProduct(pantryItem.getBarcode());
                pantryItem.enrichFromProductInfo(productInfo);
            } catch (Exception e) {
                logger.warn("Failed to lookup product info for barcode {}: {}", pantryItem.getBarcode(), e.getMessage());
            }
        }
        
        PantryItem savedItem = pantryRepository.save(pantryItem);
        logger.info("Pantry item added with ID: {}", savedItem.getId());
        
        return savedItem;
    }
    
    public List<PantryItem> getUserPantryItems(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting pantry items for user: {}", userId);
        
        List<PantryItem> items = pantryRepository.findByUserId(userId);
        
        // Sort by expiry date, expired items first, then by expiry date
        items.sort((a, b) -> {
            if (a.isExpired() && !b.isExpired()) return -1;
            if (!a.isExpired() && b.isExpired()) return 1;
            if (a.getExpiryDate() == null && b.getExpiryDate() == null) return 0;
            if (a.getExpiryDate() == null) return 1;
            if (b.getExpiryDate() == null) return -1;
            return a.getExpiryDate().compareTo(b.getExpiryDate());
        });
        
        return items;
    }
    
    public List<PantryItem> getExpiringItems(String userId, int daysThreshold) throws ExecutionException, InterruptedException {
        logger.info("Getting expiring items for user: {} within {} days", userId, daysThreshold);
        
        List<PantryItem> allItems = pantryRepository.findByUserId(userId);
        
        return allItems.stream()
                .filter(item -> !item.isFinished())
                .filter(item -> item.isExpired() || item.isNearExpiry(daysThreshold))
                .sorted(Comparator.comparing(PantryItem::getDaysUntilExpiry))
                .collect(Collectors.toList());
    }
    
    public List<PantryItem> getLowStockItems(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting low stock items for user: {}", userId);
        
        List<PantryItem> allItems = pantryRepository.findByUserId(userId);
        
        return allItems.stream()
                .filter(item -> !item.isFinished())
                .filter(PantryItem::isRunningLow)
                .collect(Collectors.toList());
    }
    
    public List<String> generateShoppingList(String userId) throws ExecutionException, InterruptedException {
        logger.info("Generating shopping list for user: {}", userId);
        
        List<PantryItem> lowStockItems = getLowStockItems(userId);
        List<PantryItem> expiringItems = getExpiringItems(userId, 7);
        
        Set<String> shoppingItems = new HashSet<>();
        
        // Add low stock items
        lowStockItems.forEach(item -> shoppingItems.add(item.getName()));
        
        // Add items that are expiring soon (to replace them)
        expiringItems.forEach(item -> shoppingItems.add(item.getName()));
        
        return new ArrayList<>(shoppingItems);
    }
    
    public PantryItem updateItem(String userId, String itemId, PantryItem updates) throws ExecutionException, InterruptedException {
        logger.info("Updating pantry item: {} for user: {}", itemId, userId);
        
        Optional<PantryItem> existingOpt = pantryRepository.findById(itemId);
        if (existingOpt.isEmpty() || !existingOpt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Pantry item not found or access denied");
        }
        
        PantryItem existing = existingOpt.get();
        
        // Update fields
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getBrand() != null) existing.setBrand(updates.getBrand());
        if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
        if (updates.getQuantity() != null) existing.setQuantity(updates.getQuantity());
        if (updates.getUnit() != null) existing.setUnit(updates.getUnit());
        if (updates.getMinimumQuantity() != null) existing.setMinimumQuantity(updates.getMinimumQuantity());
        if (updates.getPurchaseDate() != null) existing.setPurchaseDate(updates.getPurchaseDate());
        if (updates.getExpiryDate() != null) existing.setExpiryDate(updates.getExpiryDate());
        if (updates.getLocation() != null) existing.setLocation(updates.getLocation());
        if (updates.getNotes() != null) existing.setNotes(updates.getNotes());
        
        existing.updateTimestamp();
        return pantryRepository.update(existing);
    }
    
    public void markItemFinished(String userId, String itemId) throws ExecutionException, InterruptedException {
        logger.info("Marking pantry item as finished: {} for user: {}", itemId, userId);
        
        Optional<PantryItem> itemOpt = pantryRepository.findById(itemId);
        if (itemOpt.isEmpty() || !itemOpt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Pantry item not found or access denied");
        }
        
        PantryItem item = itemOpt.get();
        item.setFinished(true);
        item.updateTimestamp();
        
        pantryRepository.update(item);
    }
    
    public Map<String, Object> getPantryAnalytics(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting pantry analytics for user: {}", userId);
        
        List<PantryItem> allItems = pantryRepository.findByUserId(userId);
        
        Map<String, Object> analytics = new HashMap<>();
        
        long totalItems = allItems.stream().filter(item -> !item.isFinished()).count();
        long expiredItems = allItems.stream().filter(PantryItem::isExpired).count();
        long expiringItems = allItems.stream().filter(item -> item.isNearExpiry(7)).count();
        long lowStockItems = allItems.stream().filter(PantryItem::isRunningLow).count();
        
        analytics.put("totalItems", totalItems);
        analytics.put("expiredItems", expiredItems);
        analytics.put("expiringWithinWeek", expiringItems);
        analytics.put("lowStockItems", lowStockItems);
        
        // Category breakdown
        Map<String, Long> categoryBreakdown = allItems.stream()
                .filter(item -> !item.isFinished() && item.getCategory() != null)
                .collect(Collectors.groupingBy(PantryItem::getCategory, Collectors.counting()));
        analytics.put("categoryBreakdown", categoryBreakdown);
        
        // Estimated total value
        double totalValue = allItems.stream()
                .filter(item -> !item.isFinished() && item.getEstimatedValue() != null)
                .mapToDouble(PantryItem::getEstimatedValue)
                .sum();
        analytics.put("estimatedTotalValue", totalValue);
        
        return analytics;
    }
    
    public void scheduleExpiryNotifications(String userId) throws ExecutionException, InterruptedException {
        logger.info("Scheduling expiry notifications for user: {}", userId);
        
        List<PantryItem> expiringItems = getExpiringItems(userId, 3);
        
        for (PantryItem item : expiringItems) {
            if (item.getDaysUntilExpiry() == 1) {
                notificationService.sendExpiryAlert(userId, item, "expires_tomorrow");
            } else if (item.getDaysUntilExpiry() <= 3) {
                notificationService.sendExpiryAlert(userId, item, "expires_soon");
            }
        }
    }
    
    public PantryItem scanBarcode(String userId, String barcode) throws ExecutionException, InterruptedException {
        logger.info("Scanning barcode for user: {} - {}", userId, barcode);
        
        ProductInfo productInfo = barcodeService.lookupProduct(barcode);
        if (productInfo == null) {
            throw new IllegalArgumentException("Product not found for barcode: " + barcode);
        }
        
        PantryItem item = new PantryItem(userId, productInfo.getName(), 1.0, "piece");
        item.setBarcode(barcode);
        item.enrichFromProductInfo(productInfo);
        
        return addItem(userId, item);
    }
}