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

    public Order getNationalBestBid(){
        if (bids.isEmpty()){
            return null; 
        }

        Map.Entry<Price, LinkedBlockingQueue<Order>> bestBids = bids.firstEntry();
        if (bestBids.getValue().isEmpty()){
            return null; 
        }
        return bestBids.getValue().peek(); 
    }

    public Order getNationalBestOffer(){
        if (asks.isEmpty()){
            return null; 
        }
        Map.Entry<Price, LinkedBlockingQueue<Order>> bestAsks = asks.firstEntry(); 
        if (bestAsks.getValue().isEmpty()){
            return null; 
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
    private void executeMarketOrder(Order o){
        if (o.isBid()){
                while (!o.isCompleted()){
                    Order bestOffer = getNationalBestOffer(); 
                    if (bestOffer==null){
                        break; 
                    }
                    executeTrade(bestOffer, o);
                }
            }
        else{
            while (!o.isCompleted()){
                Order bestAsk = getNationalBestBid(); 
                if (bestAsk==null){
                    break; 
                }        
                executeTrade(o, bestAsk);
            }
        }
    }

    private void executeLimitOrder(Order o){

        while (!o.isCompleted()){
            Order bestMatch = o.isBid()? getNationalBestOffer(): getNationalBestBid(); 

            if (bestMatch == null){ break;}
            if (canTrade(o, bestMatch)){
                executeTrade(o, bestMatch);
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

    public void setTick(BigDecimal t){
        this.tick = t; 
    }
 
    public Asset getAsset(){
        return asset; 
    }
}
