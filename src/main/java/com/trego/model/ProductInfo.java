package com.trego.model;

import java.util.Map;

public class ProductInfo {
    
    private String barcode;
    private String name;
    private String brand;
    private String category;
    private String imageUrl;
    private NutritionFacts nutritionFacts;
    private Double estimatedPrice;
    private String description;
    private String manufacturer;
    private String packageSize;
    
    public ProductInfo() {}
    
    public ProductInfo(String barcode, String name, String brand) {
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
    }
    
    // Getters and Setters
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public NutritionFacts getNutritionFacts() { return nutritionFacts; }
    public void setNutritionFacts(NutritionFacts nutritionFacts) { this.nutritionFacts = nutritionFacts; }
    
    public Double getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(Double estimatedPrice) { this.estimatedPrice = estimatedPrice; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    
    public String getPackageSize() { return packageSize; }
    public void setPackageSize(String packageSize) { this.packageSize = packageSize; }
}