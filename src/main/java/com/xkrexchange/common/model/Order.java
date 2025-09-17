package com.xkrexchange.common.model;

public class Order extends Identifiable<Order> implements Comparable<Order>{
    /**
     * final fields as this is the Order object to be submitted to the OrderBook after client clicks CONFIRM
     */
    private final long clientId;
    private final OrderType orderType;
    private final Asset asset;

    private final boolean isBid; //true -> Buy, false -> Sell
    private final int quantity;

    private final Price executionPrice; // For LIMIT orders (and STOP if it becomes a limit)
    private final Price triggerPrice; // For STOP and STOP_LOSS orders

    private int remaining; 
    private boolean isCompleted = false; 

    /**
     * Overloaded Order constructor to initialize a LIMIT order
     * REQUIRES ot TO BE OrderType.LIMIT
     * @param cId - client's user ID
     * @param oT - order type
     * @param iB - is Buy (LONG) trade
     * @param shares - number of shares to trade
     * @param a - asset to trade on
     * @param eP - limit fulfilment price
     * @param tP - triggerPrice
     */

    //FACTORY CONSTRUCTOR
    private Order (long cId, OrderType oT, boolean iB, int shares, Asset a, Price eP, Price tP){
        this.clientId = cId;
        this.orderType = oT;
        this.isBid= iB;
        this.quantity = shares;
        this.asset = a;
        this.executionPrice =eP;
        this.triggerPrice = tP;
        this.remaining = shares; 
    }

    //LIMIT ORDER
    public static Order newLimitOrder(long cId, OrderType oT, boolean iB, int shares, Asset a, Price eP){
        assert  oT == OrderType.LIMIT;

        return new Order(cId, oT, iB, shares, a, eP, null);
    }

    //MARKET ORDER
    public static Order newMarketOrder(long cId, OrderType oT, boolean iB, int shares, Asset a){
        assert  oT == OrderType.MARKET;

        return  new Order(cId, oT, iB, shares, a, null, null);
    }

    //STOP ORDER
    public static Order newStopOrder(long cId, OrderType oT, boolean iB, int shares, Asset a, Price tP){
        assert  oT == OrderType.STOP;

        return  new Order(cId, oT, iB, shares, a, null, tP);
    }

    //STOP_LIMIT ORDER
    public static Order newStopLimitOrder(long cId, OrderType oT, boolean iB, int shares, Asset a, Price eP, Price tP){
        assert oT == OrderType.STOP_LIMIT;

        return new Order(cId, oT, iB, shares, a, eP, tP);
    }

    public boolean isCompleted(){
        return isCompleted; 
    }

    public void completeOrder(){
        isCompleted = true; 
    }

    /**
     * Update order after partial fill
     * @param filledQuantity - quantity that was filled
     */
    public void fillOrder(int filledQuantity, Price fillPrice) {
        remaining = remaining - filledQuantity;
        if (remaining <= 0) {
            remaining = 0;
            completeOrder();
        }
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use fillOrder(int, Price) instead
     */
    @Deprecated
    public void updateOrder(Order o){
        remaining = remaining - o.getQuantity();
        if (remaining==0){
            completeOrder();
        }
    }

    // Getter methods
    public long getOrderId(){
        return getId();
    }

    public long getClientId() {
        return clientId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public boolean isBid() {
        return isBid;
    }

    /**
     * Get the original quantity of the order
     * @return original order quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Get the remaining unfilled quantity
     * @return remaining quantity to be filled
     */
    public int getRemainingQuantity() {
        return remaining;
    }

    public Asset getAsset(){
        return asset;
    }

    public Price getExecutionPrice() {
        return executionPrice;
    }

    public Price getTriggerPrice() {
        return triggerPrice;
    }

    /**
     * Get the stop price for stop orders
     * For STOP orders, this is the trigger price
     * For STOP_LIMIT orders, this is also the trigger price
     * @return stop price or null if not a stop order
     */
    public Price getStopPrice() {
        if (orderType == OrderType.STOP || orderType == OrderType.STOP_LIMIT) {
            return triggerPrice;
        }
        return null;
    }

    /**
     * Implementation of Comparable interface for time-based ordering
     * Orders are compared by their ID (which represents time of creation)
     * Earlier orders (lower IDs) come before later orders (higher IDs)
     * This ensures FIFO (First-In-First-Out) ordering at each price level
     */
    @Override
    public int compareTo(Order other) throws NullPointerException{
        if (other == null) {
            throw new NullPointerException("Cannot compare to null Order");
        }
        return Long.compare(this.getId(), other.getId());
    }

    @Override
    public String toString() {
        return String.format("Order{id=%d, clientId=%d, type=%s, %s, quantity=%d/%d, asset=%s, execPrice=%s, triggerPrice=%s}",
                getId(), clientId, orderType, isBid ? "BUY" : "SELL", remaining, quantity, asset, executionPrice, triggerPrice);
    }
}
