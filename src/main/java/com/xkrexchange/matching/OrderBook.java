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

    public OrderBook(Asset a){
        super();
        this.asset = a; 
    }

    public long getOrderBookId(){
        return getId();
    }

    public LinkedBlockingQueue<Order> getNationalBestBids(){
        if (bids.isEmpty()){
            return null; 
        }

        Map.Entry<Price, LinkedBlockingQueue<Order>> bestBids = bids.firstEntry();
        if (bestBids.getValue().isEmpty()){
            return null; 
        }
        return bestBids.getValue(); 
    }

    public LinkedBlockingQueue<Order> getNationalBestOffers(){
        if (asks.isEmpty()){
            return null; 
        }
        Map.Entry<Price, LinkedBlockingQueue<Order>> bestAsks = asks.firstEntry(); 
        if (bestAsks.getValue().isEmpty()){
            return null; 
        }
        return bestAsks.getValue(); 
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

    private void executeTrade(Order o, Order bestMatch, LinkedBlockingQueue<Order> bestMatches){

        //check which order is the aggressor and which is the passive one 

        //if o penetrates the bestMatch

        if (o.getQuantity()> bestMatch.getQuantity()){
            // remove bestMatch from the Queue 
            bestMatches.poll(); 
            // complete the transaction (create a log)
            
            // update the status of o and bestMatch 
            o.updateOrder(bestMatch);
            bestMatch.completeOrder();
            
        }
        else{ 
            // update teh bestMatch object's remaining quantity object with the same params (including orderId) and quantity = bestMatch.shares - o.shares
            bestMatch.updateOrder(o);
            // complete transaction (create a log)
            // update status of o and the quantity remaining in bestMatch
            o.completeOrder();
        }
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
    private void executeMarketOrder(Order o){
        while (!o.isCompleted()){
            LinkedBlockingQueue<Order> bestMatches = o.isBid()? getNationalBestOffers(): getNationalBestBids(); 

            Order bestMatch = bestMatches.peek(); 
            if (bestMatch!=null && o.getClientId() != bestMatch.getClientId()){
                executeTrade(o, bestMatch, bestMatches); 
            }
        }

        // TODO: decide on cancel or post order if market order penetrates all liquidity 
    }

    private void executeLimitOrder(Order o){

        while (!o.isCompleted()){
            LinkedBlockingQueue<Order> bestMatches = o.isBid()? getNationalBestOffers(): getNationalBestBids(); 
            Order bestMatch = bestMatches.peek(); 
            if (bestMatch == null){ break;}
            if (canTrade(o, bestMatch)){
                executeTrade(o, bestMatch, bestMatches);
            } else {
                break; 
            }
        } if (!o.isCompleted()){
            addToBook(o);
        }
    }

    private boolean canTrade(Order o, Order bestMatch){
        if (o.isBid()){
            return (o.getExecutionPrice().compareTo(bestMatch.getExecutionPrice())>=0); 
        } else {
            return (o.getExecutionPrice().compareTo(bestMatch.getExecutionPrice())<=0); 
        }
    }

    private void addStopOrder(Order o){
        return;
    }
    private void addStopLimitOrder(Order o){
        return;
    }

    public Asset getAsset(){
        return asset; 
    }
}
