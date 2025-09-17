package com.trego.service;

import com.trego.model.NutritionFacts;
import com.trego.model.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BarcodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(BarcodeService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    
    public ProductInfo lookupProduct(String barcode) {
        logger.info("Looking up product with barcode: {}", barcode);
        
        try {
            // In a real implementation, you would call an external API like Open Food Facts
            // For now, return mock data
            ProductInfo productInfo = new ProductInfo(barcode, "Mock Product", "Mock Brand");
            productInfo.setCategory("Food");
            productInfo.setDescription("Mock product description");
            productInfo.setEstimatedPrice(5.99);
            
            // Add mock nutrition facts
            NutritionFacts nutrition = new NutritionFacts(250.0, 10.0, 30.0, 15.0);
            nutrition.setFiber(5.0);
            nutrition.setSugar(8.0);
            nutrition.setSodium(300.0);
            productInfo.setNutritionFacts(nutrition);
            
            logger.info("Product lookup successful for barcode: {}", barcode);
            return productInfo;
            
        } catch (Exception e) {
            logger.error("Failed to lookup product for barcode {}: {}", barcode, e.getMessage());
            throw new RuntimeException("Product lookup failed", e);
        }
    }
    
    public boolean validateBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }
        
        // Basic barcode validation (length and numeric)
        String cleaned = barcode.trim();
        return cleaned.matches("\\d{8,14}"); // 8-14 digit barcodes are common
    }
}