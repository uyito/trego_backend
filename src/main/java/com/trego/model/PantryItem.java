package com.trego.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class PantryItem extends BaseEntity {
    
    private String userId;
    private String name;
    private String brand;
    private String category;
    private Double quantity;
    private String unit;
    private Double minimumQuantity;
    private LocalDate purchaseDate;
    private LocalDate expiryDate;
    private String location;
    private String barcode;
    private String imageUrl;
    private NutritionFacts nutritionPer100g;
    private Double estimatedValue;
    private boolean isFinished;
    private String notes;
    
    public PantryItem() {
        super();
        this.isFinished = false;
    }
    
    public PantryItem(String userId, String name, Double quantity, String unit) {
        this();
        this.userId = userId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
    
    @Override
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = super.toFirestoreMap();
        map.put("userId", userId);
        map.put("name", name);
        map.put("brand", brand);
        map.put("category", category);
        map.put("quantity", quantity);
        map.put("unit", unit);
        map.put("minimumQuantity", minimumQuantity);
        map.put("purchaseDate", purchaseDate != null ? purchaseDate.toString() : null);
        map.put("expiryDate", expiryDate != null ? expiryDate.toString() : null);
        map.put("location", location);
        map.put("barcode", barcode);
        map.put("imageUrl", imageUrl);
        map.put("estimatedValue", estimatedValue);
        map.put("isFinished", isFinished);
        map.put("notes", notes);
        
        if (nutritionPer100g != null) {
            map.put("nutritionPer100g", nutritionPer100g.toFirestoreMap());
        }
        
        return map;
    }
    
    public static PantryItem fromFirestoreMap(Map<String, Object> data) {
        PantryItem item = new PantryItem();
        item.setId((String) data.get("id"));
        item.setUserId((String) data.get("userId"));
        item.setName((String) data.get("name"));
        item.setBrand((String) data.get("brand"));
        item.setCategory((String) data.get("category"));
        item.setQuantity(((Number) data.get("quantity")).doubleValue());
        item.setUnit((String) data.get("unit"));
        
        if (data.get("minimumQuantity") != null) {
            item.setMinimumQuantity(((Number) data.get("minimumQuantity")).doubleValue());
        }
        
        if (data.get("purchaseDate") instanceof String) {
            item.setPurchaseDate(LocalDate.parse((String) data.get("purchaseDate")));
        }
        
        if (data.get("expiryDate") instanceof String) {
            item.setExpiryDate(LocalDate.parse((String) data.get("expiryDate")));
        }
        
        item.setLocation((String) data.get("location"));
        item.setBarcode((String) data.get("barcode"));
        item.setImageUrl((String) data.get("imageUrl"));
        
        if (data.get("estimatedValue") != null) {
            item.setEstimatedValue(((Number) data.get("estimatedValue")).doubleValue());
        }
        
        item.setFinished((Boolean) data.getOrDefault("isFinished", false));
        item.setNotes((String) data.get("notes"));
        item.setCreatedAt(BaseEntity.timestampToLocalDateTime(data.get("createdAt")));
        item.setUpdatedAt(BaseEntity.timestampToLocalDateTime(data.get("updatedAt")));
        
        if (data.get("nutritionPer100g") instanceof Map) {
            item.setNutritionPer100g(NutritionFacts.fromFirestoreMap((Map<String, Object>) data.get("nutritionPer100g")));
        }
        
        return item;
    }
    
    public long getDaysUntilExpiry() {
        if (expiryDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
    
    public boolean isExpired() {
        if (expiryDate == null) return false;
        return LocalDate.now().isAfter(expiryDate);
    }
    
    public boolean isNearExpiry(int daysThreshold) {
        if (expiryDate == null) return false;
        return getDaysUntilExpiry() <= daysThreshold && getDaysUntilExpiry() >= 0;
    }
    
    public boolean isRunningLow() {
        if (minimumQuantity == null || quantity == null) return false;
        return quantity <= minimumQuantity;
    }
    
    public void enrichFromProductInfo(ProductInfo productInfo) {
        if (productInfo != null) {
            if (this.brand == null) this.brand = productInfo.getBrand();
            if (this.category == null) this.category = productInfo.getCategory();
            if (this.imageUrl == null) this.imageUrl = productInfo.getImageUrl();
            if (this.nutritionPer100g == null) this.nutritionPer100g = productInfo.getNutritionFacts();
            if (this.estimatedValue == null) this.estimatedValue = productInfo.getEstimatedPrice();
        }
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public Double getMinimumQuantity() { return minimumQuantity; }
    public void setMinimumQuantity(Double minimumQuantity) { this.minimumQuantity = minimumQuantity; }
    
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public NutritionFacts getNutritionPer100g() { return nutritionPer100g; }
    public void setNutritionPer100g(NutritionFacts nutritionPer100g) { this.nutritionPer100g = nutritionPer100g; }
    
    public Double getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(Double estimatedValue) { this.estimatedValue = estimatedValue; }
    
    public boolean isFinished() { return isFinished; }
    public void setFinished(boolean finished) { isFinished = finished; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}