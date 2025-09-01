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
                while (!o.isCompleted()){
                    Order bestOffer = getNationalBestOffer(); 
                    executeTrade(bestOffer, o);
                }
            }
        else{
            while (!o.isCompleted()){
                Order bestAsk = getNationalBestBid(); 
                executeTrade(o, bestAsk);
            }
        }
    }

    private void executeLimitOrder(Order o) throws NullPointerException{
        if (o.isBid()){
            while(!o.isCompleted()){
                try{
                    Order bestOffer = getNationalBestOffer(); 
                    Price clientBid = o.getExecutionPrice(); 
                    if (clientBid.compareTo(bestOffer.getExecutionPrice())>=0){
                        executeTrade(o, bestOffer);
                    } else {
                        //posting any remaining quantities in the order onto the orderbook 
                        addToBook(o);
                    }
                } catch (NoLiquidityException e){
                    addToBook(o); 
                } 
            }
        } else {
            while (!o.isCompleted()){
                try{
                    Order bestBid = getNationalBestBid(); 
                    Price clientPrice = o.getExecutionPrice(); 

                    if (clientPrice.compareTo(bestBid.getExecutionPrice())<=0){
                        executeTrade(bestBid, o);
                    } else {
                        addToBook(o);
                    }
                } catch (NoLiquidityException e){
                    addToBook(o);
                }
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
