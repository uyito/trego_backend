package com.trego.controller;

import com.trego.dto.ApiResponse;
import com.trego.model.PantryItem;
import com.trego.service.PantryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pantry")
public class PantryController {
    
    private static final Logger logger = LoggerFactory.getLogger(PantryController.class);
    
    @Autowired
    private PantryService pantryService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PantryItem>>> getUserPantryItems(Principal principal) {
        try {
            logger.info("Get pantry items request from user: {}", principal.getName());
            
            List<PantryItem> items = pantryService.getUserPantryItems(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Pantry items retrieved successfully", items));
            
        } catch (Exception e) {
            logger.error("Get pantry items failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get pantry items", "PANTRY_GET_001"));
        }
    }
    
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<PantryItem>> addPantryItem(
            @RequestBody PantryItem pantryItem,
            Principal principal) {
        
        try {
            logger.info("Add pantry item request from user: {}", principal.getName());
            
            PantryItem savedItem = pantryService.addItem(principal.getName(), pantryItem);
            
            return ResponseEntity.ok(ApiResponse.success("Pantry item added successfully", savedItem));
            
        } catch (Exception e) {
            logger.error("Add pantry item failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to add pantry item", "PANTRY_ADD_001"));
        }
    }
    
    @PostMapping("/scan-barcode")
    public ResponseEntity<ApiResponse<PantryItem>> scanBarcode(
            @RequestBody Map<String, Object> request,
            Principal principal) {
        
        try {
            String barcode = (String) request.get("barcode");
            logger.info("Scan barcode request from user: {} for barcode: {}", principal.getName(), barcode);
            
            PantryItem item = pantryService.scanBarcode(principal.getName(), barcode);
            
            return ResponseEntity.ok(ApiResponse.success("Barcode scanned and item added successfully", item));
            
        } catch (Exception e) {
            logger.error("Barcode scan failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to scan barcode", "BARCODE_SCAN_001"));
        }
    }
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<PantryItem>> updatePantryItem(
            @PathVariable String itemId,
            @RequestBody PantryItem updates,
            Principal principal) {
        
        try {
            logger.info("Update pantry item request from user: {} for item: {}", principal.getName(), itemId);
            
            PantryItem updatedItem = pantryService.updateItem(principal.getName(), itemId, updates);
            
            return ResponseEntity.ok(ApiResponse.success("Pantry item updated successfully", updatedItem));
            
        } catch (Exception e) {
            logger.error("Update pantry item failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update pantry item", "PANTRY_UPDATE_001"));
        }
    }
    
    @PostMapping("/items/{itemId}/finished")
    public ResponseEntity<ApiResponse<String>> markItemFinished(
            @PathVariable String itemId,
            Principal principal) {
        
        try {
            logger.info("Mark item finished request from user: {} for item: {}", principal.getName(), itemId);
            
            pantryService.markItemFinished(principal.getName(), itemId);
            
            return ResponseEntity.ok(ApiResponse.success("Item marked as finished", "Item marked as finished successfully"));
            
        } catch (Exception e) {
            logger.error("Mark item finished failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to mark item as finished", "PANTRY_FINISHED_001"));
        }
    }
    
    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<PantryItem>>> getExpiringItems(
            @RequestParam(defaultValue = "7") int days,
            Principal principal) {
        
        try {
            logger.info("Get expiring items request from user: {} within {} days", principal.getName(), days);
            
            List<PantryItem> expiringItems = pantryService.getExpiringItems(principal.getName(), days);
            
            return ResponseEntity.ok(ApiResponse.success("Expiring items retrieved successfully", expiringItems));
            
        } catch (Exception e) {
            logger.error("Get expiring items failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get expiring items", "PANTRY_EXPIRING_001"));
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<PantryItem>>> getLowStockItems(Principal principal) {
        try {
            logger.info("Get low stock items request from user: {}", principal.getName());
            
            List<PantryItem> lowStockItems = pantryService.getLowStockItems(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Low stock items retrieved successfully", lowStockItems));
            
        } catch (Exception e) {
            logger.error("Get low stock items failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get low stock items", "PANTRY_LOW_STOCK_001"));
        }
    }
    
    @PostMapping("/shopping-list/generate")
    public ResponseEntity<ApiResponse<List<String>>> generateShoppingList(Principal principal) {
        try {
            logger.info("Generate shopping list request from user: {}", principal.getName());
            
            List<String> shoppingList = pantryService.generateShoppingList(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Shopping list generated successfully", shoppingList));
            
        } catch (Exception e) {
            logger.error("Generate shopping list failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate shopping list", "SHOPPING_LIST_001"));
        }
    }
    
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPantryAnalytics(Principal principal) {
        try {
            logger.info("Get pantry analytics request from user: {}", principal.getName());
            
            Map<String, Object> analytics = pantryService.getPantryAnalytics(principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Pantry analytics retrieved successfully", analytics));
            
        } catch (Exception e) {
            logger.error("Get pantry analytics failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get pantry analytics", "PANTRY_ANALYTICS_001"));
        }
    }
}