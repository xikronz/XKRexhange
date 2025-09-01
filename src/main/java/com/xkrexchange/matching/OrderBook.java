package com.xkrexchange.matching;
import java.math.BigDecimal;
import java.util.*;

import com.xkrexchange.common.model.*;
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

    private Asset asset; 

    private BigDecimal tick; 

    public OrderBook(Asset a){
        super();
        tick = new BigDecimal("0.01");
        this.asset = a; 
    }

    public OrderBook(BigDecimal t){
        super();
        tick = t; 
    }

    public long getOrderBookId(){
        return getId();
    }

    public Order getNationalBestBid() throws NoLiquidityException, MissingOrderException, NullPointerException {
        if (bids.isEmpty()){
            throw new NoLiquidityException(String.format("No liquidity on bids available for %s", getAsset())); 
        }

        Map.Entry<Price, LinkedBlockingQueue<Order>> bestBids = bids.firstEntry();
        
        if (bestBids.getValue().isEmpty()){
            throw new MissingOrderException(String.format("Asset {%s} trading at ${%s} has no current bid orders", getAsset(), bestBids.getKey())); 
        }

        return bestBids.getValue().peek(); 
    }

    public Order getNationalBestOffer() throws NoLiquidityException, MissingOrderException, NullPointerException{
        if (asks.isEmpty()){
            throw new NoLiquidityException(String.format("No liquidity on asks available for %s", getAsset()));
        }

        Map.Entry<Price, LinkedBlockingQueue<Order>> bestAsks = asks.firstEntry(); 

        if (bestAsks.getValue().isEmpty()){
            throw new MissingOrderException(String.format("Asset {%s} trading at ${%s} has no current bid orders", getAsset(), bestAsks.getKey()));
        }

        return bestAsks.getValue().peek(); 
    }

    public void submitOrder(Order o){
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

    private void addToBook(Order o){
        Price orderPrice = o.getExecutionPrice(); 
        if (o.isBid()){
            bids.computeIfAbsent(orderPrice, k -> new LinkedBlockingQueue<>()).add(o); 
        } else {
            asks.computeIfAbsent(orderPrice, k-> new LinkedBlockingQueue<>()).add(o); 
        }
    }

    //HIGHEST EXECUTION PRIORITY 
    private void executeMarketOrder(Order o) throws NoLiquidityException, MissingOrderException, NullPointerException{
        if (o.isBid()){
                while (!o.isFullyFilled()){
                    Order bestOffer = getNationalBestOffer(); 
                    executeTrade(bestOffer, o);
                }
            }
        else{
            while (!o.isFullyFilled()){
                Order bestAsk = getNationalBestBid(); 
                executeTrade(o, bestAsk);
            }
        }
    }

    private void executeLimitOrder(Order o) throws NullPointerException{
        if (o.getExecutionPrice().equals(new BigDecimal("0"))){
            throw new UnsupportedOperationException(); 
        }
        if (o.isBid()){
            while(!o.isFullyFilled()){
                try{
                    Order bestOffer = getNationalBestOffer(); 
                    executeTrade(o, bestOffer);
                } catch (NoLiquidityException e){
                    addToBook(o); 
                }
            }
        }
    }



    private void executeLimitOrderS(Order o){
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
                addToBook(o);
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
                addToBook(o);
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
 
    public Asset getAsset(){
        return asset; 
    }
}
