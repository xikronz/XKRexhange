package com.xkrexchange.common.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CompletedTrade - Immutable record of an executed trade
 * 
 * This class represents a completed trade between two orders and provides
 * a complete audit trail for regulatory compliance and trade history.
 * 
 * Each trade records the exact details of the execution including:
 * -> Which orders were matched
 * -> The execution price and quantity
 * -> Timestamp of execution
 * -> Asset being traded
 * -> User information for both sides
 */
public class CompletedTrade extends Identifiable<CompletedTrade> {
    
    // Trade Identification
    private final long tradeId;
    private final LocalDateTime executionTimestamp;
    
    // Order Information
    private final long buyOrderId;
    private final long sellOrderId;
    private final long buyerUserId;
    private final long sellerUserId;
    
    // Asset and Execution Details
    private final long assetId;
    private final String assetTicker;
    private final int quantity;
    private final Price executionPrice;
    private final BigDecimal totalValue;
    
    // Trade Classification
    private final OrderType buyOrderType;
    private final OrderType sellOrderType;
    private final boolean wasMarketOrder; // True if either side was market order
    
    /**
     * Constructor for CompletedTrade
     * 
     * @param buyOrder The buy order that was matched
     * @param sellOrder The sell order that was matched
     * @param executionPrice The price at which the trade occurred
     * @param quantity The number of shares/units traded
     */
    public CompletedTrade(Order buyOrder, Order sellOrder, Price executionPrice, int quantity) {
        super(); // Generate unique trade ID
        
        this.tradeId = getId();
        this.executionTimestamp = LocalDateTime.now();
        
        // Order details
        this.buyOrderId = buyOrder.getId();
        this.sellOrderId = sellOrder.getId();
        this.buyerUserId = buyOrder.getClientId();
        this.sellerUserId = sellOrder.getClientId();
        
        // Asset details
        this.assetId = buyOrder.getAsset().getId();
        this.assetTicker = buyOrder.getAsset().getTicker();
        
        // Execution details
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.totalValue = executionPrice.getValue().multiply(BigDecimal.valueOf(quantity));
        
        // Trade classification
        this.buyOrderType = buyOrder.getOrderType();
        this.sellOrderType = sellOrder.getOrderType();
        this.wasMarketOrder = (buyOrderType == OrderType.MARKET || sellOrderType == OrderType.MARKET);
    }
    
    // Getters for all fields
    public long getTradeId() { return tradeId; }
    public LocalDateTime getExecutionTimestamp() { return executionTimestamp; }
    
    public long getBuyOrderId() { return buyOrderId; }
    public long getSellOrderId() { return sellOrderId; }
    public long getBuyerUserId() { return buyerUserId; }
    public long getSellerUserId() { return sellerUserId; }
    
    public long getAssetId() { return assetId; }
    public String getAssetTicker() { return assetTicker; }
    public int getQuantity() { return quantity; }
    public Price getExecutionPrice() { return executionPrice; }
    public BigDecimal getTotalValue() { return totalValue; }
    
    public OrderType getBuyOrderType() { return buyOrderType; }
    public OrderType getSellOrderType() { return sellOrderType; }
    public boolean wasMarketOrder() { return wasMarketOrder; }
    
    /**
     * Get the aggressive order (the one that caused the trade)
     * Market orders are always aggressive, otherwise it's the later arriving order
     */
    public long getAggressiveOrderId() {
        if (buyOrderType == OrderType.MARKET) return buyOrderId;
        if (sellOrderType == OrderType.MARKET) return sellOrderId;
        // For limit orders, we'd need order timestamps to determine aggressor
        // For now, assume buy order is aggressive (this could be enhanced)
        return buyOrderId;
    }
    
    /**
     * Get the passive order (the one that was resting on the book)
     */
    public long getPassiveOrderId() {
        return (getAggressiveOrderId() == buyOrderId) ? sellOrderId : buyOrderId;
    }
    
    /**
     * Calculate the trade value in basis points (for analytics)
     * Useful for analyzing price movements and market impact
     */
    public BigDecimal getTradeValueBasisPoints() {
        // This would require previous price data to calculate price movement
        // For now, return total trade value
        return totalValue;
    }
    
    /**
     * Determine if this was a "block trade" (large institutional trade)
     * Typically trades over certain thresholds (e.g., $200k or 10,000 shares)
     */
    public boolean isBlockTrade() {
        return quantity >= 10000 || totalValue.compareTo(new BigDecimal("200000")) >= 0;
    }
    
    /**
     * Create a trade summary for reporting/logging
     */
    public String getTradeDescription() {
        return String.format("Trade %d: %s %d shares of %s at %s (Total: %s)", 
                           tradeId, 
                           wasMarketOrder ? "MARKET" : "LIMIT",
                           quantity, 
                           assetTicker, 
                           executionPrice.getValue(), 
                           totalValue);
    }
    
    @Override
    public String toString() {
        return String.format("CompletedTrade{id=%d, asset=%s, qty=%d, price=%s, time=%s}", 
                           tradeId, assetTicker, quantity, executionPrice.getValue(), executionTimestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CompletedTrade that = (CompletedTrade) obj;
        return tradeId == that.tradeId;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(tradeId);
    }
} 