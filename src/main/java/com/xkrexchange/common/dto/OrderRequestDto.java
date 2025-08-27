package com.xkrexchange.common.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for incoming order requests from frontend
 * This represents the raw data sent by the client before validation
 */
public class OrderRequestDto {
    
    private String orderType; // "MARKET", "LIMIT", "STOP", "STOP_LIMIT"
    private boolean isBuyOrder; // true for BUY, false for SELL
    private String assetTicker; // e.g., "AAPL", "TSLA"
    private Long assetId; // Internal asset ID
    private int quantity; // Number of shares
    private BigDecimal limitPrice; // For LIMIT and STOP_LIMIT orders
    private BigDecimal triggerPrice; // For STOP and STOP_LIMIT orders
    
    // Default constructor for JSON deserialization
    public OrderRequestDto() {}
    
    // Constructor for creating test instances
    public OrderRequestDto(String orderType, boolean isBuyOrder, String assetTicker, 
                          Long assetId, int quantity, BigDecimal limitPrice, BigDecimal triggerPrice) {
        this.orderType = orderType;
        this.isBuyOrder = isBuyOrder;
        this.assetTicker = assetTicker;
        this.assetId = assetId;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.triggerPrice = triggerPrice;
    }
    
    // Getters and Setters
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public boolean isBuyOrder() { return isBuyOrder; }
    public void setBuyOrder(boolean buyOrder) { isBuyOrder = buyOrder; }
    
    public String getAssetTicker() { return assetTicker; }
    public void setAssetTicker(String assetTicker) { this.assetTicker = assetTicker; }
    
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public BigDecimal getLimitPrice() { return limitPrice; }
    public void setLimitPrice(BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    
    public BigDecimal getTriggerPrice() { return triggerPrice; }
    public void setTriggerPrice(BigDecimal triggerPrice) { this.triggerPrice = triggerPrice; }
    
    // Validation helper methods
    public boolean isMarketOrder() {
        return "MARKET".equalsIgnoreCase(orderType);
    }
    
    public boolean isLimitOrder() {
        return "LIMIT".equalsIgnoreCase(orderType);
    }
    
    public boolean isStopOrder() {
        return "STOP".equalsIgnoreCase(orderType);
    }
    
    public boolean isStopLimitOrder() {
        return "STOP_LIMIT".equalsIgnoreCase(orderType);
    }
    
    /**
     * Basic validation of order request data
     * @return Validation error message, or null if valid
     */
    public String validate() {
        if (orderType == null || orderType.trim().isEmpty()) {
            return "Order type is required";
        }
        
        if (quantity <= 0) {
            return "Quantity must be positive";
        }
        
        if (assetId == null) {
            return "Asset ID is required";
        }
        
        // Validate price requirements based on order type
        if (isLimitOrder() || isStopLimitOrder()) {
            if (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return "Limit price must be positive for LIMIT and STOP_LIMIT orders";
            }
        }
        
        if (isStopOrder() || isStopLimitOrder()) {
            if (triggerPrice == null || triggerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return "Trigger price must be positive for STOP and STOP_LIMIT orders";
            }
        }
        
        return null; // Valid
    }
    
    @Override
    public String toString() {
        return String.format("OrderRequest{type=%s, side=%s, asset=%s, qty=%d, limit=%s, trigger=%s}", 
                           orderType, isBuyOrder ? "BUY" : "SELL", assetTicker, quantity, limitPrice, triggerPrice);
    }
} 