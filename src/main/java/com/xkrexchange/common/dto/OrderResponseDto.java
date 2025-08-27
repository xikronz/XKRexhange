package com.xkrexchange.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for order responses sent back to frontend
 * This represents the response after order processing (success or failure)
 */
public class OrderResponseDto {
    
    private boolean success;
    private String message;
    private Long orderId;
    private String orderStatus; // "PENDING", "FILLED", "PARTIALLY_FILLED", "REJECTED", "CANCELLED"
    private LocalDateTime timestamp;
    
    // For successful orders - execution details
    private BigDecimal executedPrice;
    private int executedQuantity;
    private BigDecimal totalValue;
    
    // Default constructor
    public OrderResponseDto() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for simple messages (errors or basic responses)
    public OrderResponseDto(String message) {
        this();
        this.success = false;
        this.message = message;
    }
    
    // Static factory methods for common responses
    public static OrderResponseDto success(Long orderId) {
        OrderResponseDto response = new OrderResponseDto();
        response.success = true;
        response.orderId = orderId;
        response.message = "Order submitted successfully";
        response.orderStatus = "PENDING";
        return response;
    }
    
    public static OrderResponseDto success(Long orderId, String message) {
        OrderResponseDto response = new OrderResponseDto();
        response.success = true;
        response.orderId = orderId;
        response.message = message;
        response.orderStatus = "PENDING";
        return response;
    }
    
    public static OrderResponseDto error(String errorMessage) {
        OrderResponseDto response = new OrderResponseDto();
        response.success = false;
        response.message = errorMessage;
        return response;
    }
    
    public static OrderResponseDto executionUpdate(Long orderId, BigDecimal executedPrice, 
                                                  int executedQuantity, String status) {
        OrderResponseDto response = new OrderResponseDto();
        response.success = true;
        response.orderId = orderId;
        response.executedPrice = executedPrice;
        response.executedQuantity = executedQuantity;
        response.totalValue = executedPrice.multiply(BigDecimal.valueOf(executedQuantity));
        response.orderStatus = status;
        response.message = String.format("Executed %d shares at $%s", executedQuantity, executedPrice);
        return response;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public void setExecutedPrice(BigDecimal executedPrice) { this.executedPrice = executedPrice; }
    
    public int getExecutedQuantity() { return executedQuantity; }
    public void setExecutedQuantity(int executedQuantity) { this.executedQuantity = executedQuantity; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    @Override
    public String toString() {
        if (success && orderId != null) {
            return String.format("OrderResponse{success=true, orderId=%d, status=%s, message='%s'}", 
                               orderId, orderStatus, message);
        } else {
            return String.format("OrderResponse{success=false, message='%s'}", message);
        }
    }
} 