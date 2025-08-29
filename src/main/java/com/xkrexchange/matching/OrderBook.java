package com.xkrexchange.matching;
import java.math.BigDecimal;
import java.util.*;

import com.xkrexchange.common.model.Price;
import com.xkrexchange.common.model.Identifiable;
import com.xkrexchange.common.model.Order;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;


/** Logic behind matching orders and keeping track of NBBO prices so that Pro-Rata Priority can be implemented when fulfilling orders
 * CLASS INVARIANT: Orderbooks must be Assigned to a UNIQUE asset object only
 */

public class OrderBook extends Identifiable<OrderBook>{
    // Bid side: Highest prices first (descending order)
    private ConcurrentSkipListMap<Price, LinkedBlockingQueue<Order>> bids = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    // Ask side: Lowest prices first (ascending order) 
    private ConcurrentSkipListMap<Price, LinkedBlockingQueue<Order>> asks = new ConcurrentSkipListMap<>();

    private BigDecimal tick; 

    public OrderBook(){
        super();
        tick = new BigDecimal("0.01");
    }

    public OrderBook(BigDecimal t){
        super();
        tick = t; 
    }

    public long getOrderBookId(){
        return getId();
    }

    public Order getNationalBestBid(){
        Map.Entry<Price, LinkedBlockingQueue<Order>> bestEntry = this.bids.firstEntry();
        if (bestEntry == null || bestEntry.getValue().isEmpty()) {
            return null;
        }
        return bestEntry.getValue().peek(); // Get first order at best price
    }

    public Order getNationalBestOffer(){
        Map.Entry<Price, LinkedBlockingQueue<Order>> bestEntry = this.asks.firstEntry(); 

        if (bestEntry == null || bestEntry.getValue().isEmpty()) {
            return null;
        }
        return bestEntry.getValue().peek();
    }

    public void addOrder(Order o){
        switch (o.getOrderType()){
            case MARKET:
                executeMarketOrder(o);
            case LIMIT:
                executeLimitOrder(o);
            case STOP:
                addStopOrder(o);
            case STOP_LIMIT:
                addStopLimitOrder(o);

        }
    }

    private void executeTrade(Order bid, Order ask){
        return;
    }

    //HIGHEST EXECUTION PRIORITY 
    private void executeMarketOrder(Order o) throws NoLiquidityException{
        if (o.isBid()){
            if (asks.isEmpty()){
                throw new NoLiquidityException(String.format("There are no asks listed to fulfill Market Buy Order on %s", o.getAsset())); 
            }
                while (!o.isFullyFilled()){
                    Order bestOffer = getNationalBestOffer(); 
                    executeTrade(bestOffer, o);
                }
            }
        else{
            if (bids.isEmpty()){
                throw new NoLiquidityException(String.format("There are no bids listed to fulfill the Market Sell Order on %s", o.getAsset()));
            }
            while (!o.isFullyFilled()){
                Order bestAsk = getNationalBestBid(); 
                executeTrade(o, bestAsk);
            }
        }
    }

    private void executeLimitOrder(Order o){
        if (o.isBid()){
            // BUY LIMIT: Try to match against asks at or below limit price
            while (!o.isFullyFilled() && !asks.isEmpty()) {
                Map.Entry<Price, LinkedBlockingQueue<Order>> bestAsk = asks.firstEntry();
                Price askPrice = bestAsk.getKey();
                
                // Can only match if ask price <= limit price (favorable for buyer)
                if (askPrice.compareTo(o.getExecutionPrice()) > 0) {
                    break; // No more favorable matches available
                }
                
                Order bestOffer = bestAsk.getValue().peek();
                if (bestOffer != null) {
                    executeTrade(o, bestOffer);
                    
                    // Remove fully filled orders from book
                    if (bestOffer.isFullyFilled()) {
                        bestAsk.getValue().poll();
                        if (bestAsk.getValue().isEmpty()) {
                            asks.remove(askPrice);
                        }
                    }
                }
            }
            
            // Post any remaining quantity to bid book
            if (!o.isFullyFilled()) {
                addToBook(o, bids);
            }
            
        } else {
            // SELL LIMIT: Try to match against bids at or above limit price
            while (!o.isFullyFilled() && !bids.isEmpty()) {
                Map.Entry<Price, LinkedBlockingQueue<Order>> bestBid = bids.firstEntry();
                Price bidPrice = bestBid.getKey();
                
                // Can only match if bid price >= limit price (favorable for seller)
                if (bidPrice.compareTo(o.getExecutionPrice()) < 0) {
                    break; // No more favorable matches available
                }
                
                Order bestBid_order = bestBid.getValue().peek();
                if (bestBid_order != null) {
                    executeTrade(bestBid_order, o);
                    
                    // Remove fully filled orders from book
                    if (bestBid_order.isFullyFilled()) {
                        bestBid.getValue().poll();
                        if (bestBid.getValue().isEmpty()) {
                            bids.remove(bidPrice);
                        }
                    }
                }
            }
            
            // Post any remaining quantity to ask book
            if (!o.isFullyFilled()) {
                addToBook(o, asks);
            }
        }
    }

    private void addStopOrder(Order o){
        return;
    }
    private void addStopLimitOrder(Order o){
        return;
    }

    public void setTick(BigDecimal t){
        this.tick = t; 
    }
    
    /**
     * Validates that a price is a valid multiple of the tick size
     * @param price Price to validate
     * @throws IllegalArgumentException if price is not a valid tick multiple
     */
    private void validateTickSize(Price price) throws IllegalArgumentException{
        BigDecimal priceValue = price.getValue(); // Assuming Price has getValue() method
        
        // Check if price is a multiple of tick size
        BigDecimal remainder = priceValue.remainder(tick);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                String.format("Price %s is not a valid multiple of tick size %s. " +
                            "Valid price must be divisible by %s", 
                            priceValue, tick, tick));
        }
    }
    
    /**
     * Helper method to validate and add order to the appropriate book side
     * @param order Order to add
     * @param book The book (bids or asks) to add to
     */
    private void addToBook(Order order, ConcurrentSkipListMap<Price, LinkedBlockingQueue<Order>> book) {
        Price orderPrice = order.getExecutionPrice();
        
        validateTickSize(orderPrice);
        book.computeIfAbsent(orderPrice, k -> new LinkedBlockingQueue<>()).add(order);
    }

}
