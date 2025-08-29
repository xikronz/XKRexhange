package com.xkrexchange.matching;
import java.util.*;

import com.xkrexchange.common.model.Price;
import com.xkrexchange.common.model.Identifiable;
import com.xkrexchange.common.model.Order;
import java.util.concurrent.ConcurrentSkipListMap;

/** Logic behind matching orders and keeping track of NBBO prices so that Pro-Rata Priority can be implemented when fulfilling orders
 * CLASS INVARIANT: Orderbooks must be Assigned to a UNIQUE asset object only
 */

public class OrderBook extends Identifiable<OrderBook>{
    // Bid side: Highest prices first (descending order)
    private ConcurrentSkipListMap<Price, PriorityQueue<Order>> bids = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    // Ask side: Lowest prices first (ascending order) 
    private ConcurrentSkipListMap<Price, PriorityQueue<Order>> asks = new ConcurrentSkipListMap<>();

    public OrderBook(){
        super();
    }

    public long getOrderBookId(){
        return getId();
    }

    public Order getNationalBestBid(){
        Map.Entry<Price, PriorityQueue<Order>> bestEntry = this.bids.firstEntry();
        if (bestEntry == null || bestEntry.getValue().isEmpty()) {
            return null;
        }
        return bestEntry.getValue().peek(); // Get first order at best price
    }

    public Order getNationalBestOffer(){
        Map.Entry<Price, PriorityQueue<Order>> bestEntry = this.asks.firstEntry(); 

        if (bestEntry == null || bestEntry.getValue().isEmpty()) {
            return null;
        }
        return bestEntry.getValue().peek();
    }
}
