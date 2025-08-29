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
    private boolean isFullyFilled = false; 

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
    public Order newLimitOrder(long cId, OrderType oT, boolean iB, int shares, Asset a, Price eP){
        assert  oT == OrderType.LIMIT;

        return new Order(cId, oT, iB, shares, a, eP, null);
    }

    //MARKET ORDER
    public Order newMarketOrder(long cId, OrderType oT, boolean iB, int shares, Asset a){
        assert  oT == OrderType.MARKET;

        return  new Order(cId, oT, iB, shares, a, null, null);
    }

    //STOP ORDER
    public Order newStopOrder(long cId, OrderType oT, boolean iB, int shares, Asset a, Price tP){
        assert  oT == OrderType.STOP;

        return  new Order(cId, oT, iB, shares, a, null, tP);
    }

    //STOP_LIMIT ORDER
    public Order newStopLimitOrder(long cId, OrderType oT, boolean iB, int shares, Asset a, Price eP, Price tP){
        assert oT == OrderType.STOP_LIMIT;

        return new Order(cId, oT, iB, shares, a, eP, tP);
    }

    public boolean isFullyFilled(){
        return isFullyFilled; 
    }

    public void updateOrder(Order o){
        remaining = remaining - o.quantity;
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

    public int getQuantity() {
        return quantity;
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
        return String.format("Order{id=%d, clientId=%d, type=%s, %s, quantity=%d, asset=%s, execPrice=%s, triggerPrice=%s}",
                getId(), clientId, orderType, isBid ? "BUY" : "SELL", quantity, asset, executionPrice, triggerPrice);
    }
}
