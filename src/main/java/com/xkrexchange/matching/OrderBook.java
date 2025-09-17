package com.xkrexchange.matching;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

import com.xkrexchange.common.model.*;

/** 
 * Singular orderbook that targets  with global order queue and time priority
 * CLASS INVARIANT: Orderbooks must be Assigned to a UNIQUE asset object only
 * 
 * Architecture:
 * 1. All orders go through a global orderQueue (FIFO time priority)
 * 2. Single matching engine thread processes orders sequentially
 * 3. Stop orders are held in separate queues until triggered
 * 4. Price updates from trades trigger stop order conversions
 */
public class OrderBook extends Identifiable<OrderBook> {
    
    // === ORDER BOOKS ===
    // Bid side: Highest prices first (descending order)
    private ConcurrentSkipListMap<Price, LinkedBlockingQueue<Order>> bids = new ConcurrentSkipListMap<>(Collections.reverseOrder());
    // Ask side: Lowest prices first (ascending order) 
    private ConcurrentSkipListMap<Price, LinkedBlockingQueue<Order>> asks = new ConcurrentSkipListMap<>();

    // === GLOBAL ORDER QUEUE (FIFO Time Priority) ===
    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
    
    // === STOP ORDER QUEUES (Price + Time Priority) ===
    // Buy stops: ascending by stop price (lowest triggers first)
    private final PriorityQueue<Order> buyStopQueue = new PriorityQueue<>(
        Comparator.comparing(Order::getStopPrice)
                  .thenComparing(Order::getOrderId) // time priority
    );
    
    // Sell stops: descending by stop price (highest triggers first)  
    private final PriorityQueue<Order> sellStopQueue = new PriorityQueue<>(
        Comparator.comparing(Order::getStopPrice, Collections.reverseOrder())
                  .thenComparing(Order::getOrderId) // time priority
    );
    
    // Stop-limit orders (similar structure)
    private final PriorityQueue<Order> buyStopLimitQueue = new PriorityQueue<>(
        Comparator.comparing(Order::getStopPrice)
                  .thenComparing(Order::getOrderId)
    );
    
    private final PriorityQueue<Order> sellStopLimitQueue = new PriorityQueue<>(
        Comparator.comparing(Order::getStopPrice, Collections.reverseOrder())
                  .thenComparing(Order::getOrderId)
    );

    // === TRADE TRACKING ===
    private volatile Price lastTradePrice = null; // Updated after each trade
    private final List<CompletedTrade> tradeHistory = new ArrayList<>();
    
    // === MATCHING ENGINE CONTROL ===
    private volatile boolean isRunning = false;
    private Thread matchingEngineThread;
    
    private Asset asset;

    public OrderBook(Asset a) {
        super();
        this.asset = a;
    }

    // === PUBLIC API ===
    
    /**
     * Submit order to the global queue (thread-safe)
     * No immediate matching - just enqueues for processing
     */
    public void submitOrder(Order order) {
        try {
            orderQueue.put(order); // Blocking if queue is full
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order submission interrupted", e);
        }
    }
    
    /**
     * Start the matching engine (single background thread)
     */
    public void startMatchingEngine() {
        if (isRunning) return;
        
        isRunning = true;
        matchingEngineThread = new Thread(this::matchingEngineLoop);
        matchingEngineThread.setName("MatchingEngine-" + asset.getTicker());
        matchingEngineThread.setDaemon(true);
        matchingEngineThread.start();
    }
    
    /**
     * Stop the matching engine gracefully
     */
    public void stopMatchingEngine() {
        isRunning = false;
        if (matchingEngineThread != null) {
            matchingEngineThread.interrupt();
        }
    }

    // === MATCHING ENGINE CORE ===
    
    /**
     * Main matching engine loop - processes orders sequentially
     * This ensures proper time priority and thread safety
     */
    private void matchingEngineLoop() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Blocking wait for next order (FIFO time priority)
                Order order = orderQueue.take();
                
                // Route order based on type
                switch (order.getOrderType()) {
                    case MARKET -> executeMarketOrder(order);
                    case LIMIT -> executeLimitOrder(order);
                    case STOP -> addToStopQueue(order);
                    case STOP_LIMIT -> addToStopLimitQueue(order);
                }
                
                // After processing any order, check if stops should be triggered
                if (lastTradePrice != null) {
                    checkStopTriggers();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log error but keep engine running
                System.err.println("Error in matching engine: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // === ORDER EXECUTION ===
    
    /**
     * Execute market order immediately against best available liquidity
     */
    private void executeMarketOrder(Order order) {
        while (!order.isCompleted()) {
            LinkedBlockingQueue<Order> opposingSide = order.isBid() ? 
                getNationalBestOffers() : getNationalBestBids();
            
            if (opposingSide == null || opposingSide.isEmpty()) {
                // No liquidity available - cancel or convert to limit order
                // For now, we'll just complete the order (partial fill)
                break;
            }
            
            Order bestMatch = opposingSide.peek();
            if (bestMatch != null && order.getClientId() != bestMatch.getClientId()) {
                executeTrade(order, bestMatch, opposingSide);
            } else {
                break;
            }
        }
    }
    
    /**
     * Execute limit order - try to match first, then post to book
     */
    private void executeLimitOrder(Order order) {
        // First, try to match against existing orders
        while (!order.isCompleted()) {
            LinkedBlockingQueue<Order> opposingSide = order.isBid() ? 
                getNationalBestOffers() : getNationalBestBids();
            
            if (opposingSide == null || opposingSide.isEmpty()) {
                break;
            }
            
            Order bestMatch = opposingSide.peek();
            if (bestMatch == null) {
                break;
            }
            
            // Check if we can trade (price compatibility)
            if (canTrade(order, bestMatch) && order.getClientId() != bestMatch.getClientId()) {
                executeTrade(order, bestMatch, opposingSide);
            } else {
                break;
            }
        }
        
        // If order is not fully filled, add remaining quantity to book
        if (!order.isCompleted()) {
            addToBook(order);
        }
    }
    
    /**
     * Execute a trade between two orders
     */
    private void executeTrade(Order aggressiveOrder, Order passiveOrder, LinkedBlockingQueue<Order> passiveQueue) {
        // Determine trade quantity (minimum of both orders)
        int tradeQuantity = Math.min(aggressiveOrder.getRemainingQuantity(), 
                                   passiveOrder.getRemainingQuantity());
        
        // Trade price is always the passive order's price (price-time priority)
        Price tradePrice = passiveOrder.getExecutionPrice();
        
        // Update order quantities
        aggressiveOrder.fillOrder(tradeQuantity, tradePrice);
        passiveOrder.fillOrder(tradeQuantity, tradePrice);
        
        // Remove passive order from queue if fully filled
        if (passiveOrder.isCompleted()) {
            passiveQueue.poll();
            // Clean up empty price levels
            cleanupEmptyPriceLevel(passiveOrder, passiveQueue);
        }
        
        // Update last trade price (triggers stop checks)
        lastTradePrice = tradePrice;
        
        // Record trade for audit trail
        recordTrade(aggressiveOrder, passiveOrder, tradePrice, tradeQuantity);
    }
    
    /**
     * Record completed trade in history
     */
    private void recordTrade(Order buyOrder, Order sellOrder, Price price, int quantity) {
        // Determine which is buy vs sell based on order side
        Order actualBuyOrder = buyOrder.isBid() ? buyOrder : sellOrder;
        Order actualSellOrder = buyOrder.isBid() ? sellOrder : buyOrder;
        
        CompletedTrade trade = new CompletedTrade(actualBuyOrder, actualSellOrder, price, quantity);
        tradeHistory.add(trade);
    }

    // === STOP ORDER MANAGEMENT ===
    
    /**
     * Add stop order to appropriate queue
     */
    private void addToStopQueue(Order order) {
        if (order.isBid()) {
            buyStopQueue.offer(order);
        } else {
            sellStopQueue.offer(order);
        }
    }
    
    /**
     * Add stop-limit order to appropriate queue
     */
    private void addToStopLimitQueue(Order order) {
        if (order.isBid()) {
            buyStopLimitQueue.offer(order);
        } else {
            sellStopLimitQueue.offer(order);
        }
    }
    
    /**
     * Check if any stop orders should be triggered based on last trade price
     */
    private void checkStopTriggers() {
        if (lastTradePrice == null) return;
        
        // Check buy stop orders (trigger when price >= stop price)
        checkBuyStopTriggers();
        
        // Check sell stop orders (trigger when price <= stop price)  
        checkSellStopTriggers();
        
        // Check stop-limit orders
        checkBuyStopLimitTriggers();
        checkSellStopLimitTriggers();
    }
    
    private void checkBuyStopTriggers() {
        while (!buyStopQueue.isEmpty()) {
            Order stopOrder = buyStopQueue.peek();
            
            // Buy stop triggers when market price >= stop price
            if (lastTradePrice.compareTo(stopOrder.getStopPrice()) >= 0) {
                buyStopQueue.poll();
                
                // Convert to market order and re-inject into queue
                Order marketOrder = Order.newMarketOrder(
                    stopOrder.getClientId(),
                    OrderType.MARKET,
                    true, // buy
                    stopOrder.getRemainingQuantity(),
                    stopOrder.getAsset()
                );
                
                try {
                    orderQueue.put(marketOrder);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                // Stop orders are sorted, so if this one doesn't trigger, none after it will
                break;
            }
        }
    }
    
    private void checkSellStopTriggers() {
        while (!sellStopQueue.isEmpty()) {
            Order stopOrder = sellStopQueue.peek();
            
            // Sell stop triggers when market price <= stop price
            if (lastTradePrice.compareTo(stopOrder.getStopPrice()) <= 0) {
                sellStopQueue.poll();
                
                // Convert to market order and re-inject into queue
                Order marketOrder = Order.newMarketOrder(
                    stopOrder.getClientId(),
                    OrderType.MARKET,
                    false, // sell
                    stopOrder.getRemainingQuantity(),
                    stopOrder.getAsset()
                );
                
                try {
                    orderQueue.put(marketOrder);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break;
            }
        }
    }
    
    private void checkBuyStopLimitTriggers() {
        while (!buyStopLimitQueue.isEmpty()) {
            Order stopLimitOrder = buyStopLimitQueue.peek();
            
            if (lastTradePrice.compareTo(stopLimitOrder.getStopPrice()) >= 0) {
                buyStopLimitQueue.poll();
                
                // Convert to limit order and re-inject into queue
                Order limitOrder = Order.newLimitOrder(
                    stopLimitOrder.getClientId(),
                    OrderType.LIMIT,
                    true, // buy
                    stopLimitOrder.getRemainingQuantity(),
                    stopLimitOrder.getAsset(),
                    stopLimitOrder.getExecutionPrice() // use limit price
                );
                
                try {
                    orderQueue.put(limitOrder);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break;
            }
        }
    }
    
    private void checkSellStopLimitTriggers() {
        while (!sellStopLimitQueue.isEmpty()) {
            Order stopLimitOrder = sellStopLimitQueue.peek();
            
            if (lastTradePrice.compareTo(stopLimitOrder.getStopPrice()) <= 0) {
                sellStopLimitQueue.poll();
                
                // Convert to limit order and re-inject into queue
                Order limitOrder = Order.newLimitOrder(
                    stopLimitOrder.getClientId(),
                    OrderType.LIMIT,
                    false, // sell
                    stopLimitOrder.getRemainingQuantity(),
                    stopLimitOrder.getAsset(),
                    stopLimitOrder.getExecutionPrice() // use limit price
                );
                
                try {
                    orderQueue.put(limitOrder);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break;
            }
        }
    }

    // helpers
    
    /**
     * Add order to the appropriate order book
     */
    private void addToBook(Order order) {
        Price orderPrice = order.getExecutionPrice();
        if (order.isBid()) {
            bids.computeIfAbsent(orderPrice, k -> new LinkedBlockingQueue<>()).add(order);
        } else {
            asks.computeIfAbsent(orderPrice, k -> new LinkedBlockingQueue<>()).add(order);
        }
    }
    
    /**
     * Check if two orders can trade based on price compatibility
     */
    private boolean canTrade(Order aggressiveOrder, Order passiveOrder) {
        if (aggressiveOrder.isBid()) {
            // Buy order can trade if bid price >= ask price
            return aggressiveOrder.getExecutionPrice().compareTo(passiveOrder.getExecutionPrice()) >= 0;
        } else {
            // Sell order can trade if ask price <= bid price
            return aggressiveOrder.getExecutionPrice().compareTo(passiveOrder.getExecutionPrice()) <= 0;
        }
    }
    
    /**
     * Remove empty price levels from order book
     */
    private void cleanupEmptyPriceLevel(Order order, LinkedBlockingQueue<Order> queue) {
        if (queue.isEmpty()) {
            Price price = order.getExecutionPrice();
            if (order.isBid()) {
                bids.remove(price);
            } else {
                asks.remove(price);
            }
        }
    }

    // NBBO METHODS 
    
    public LinkedBlockingQueue<Order> getNationalBestBids() {
        if (bids.isEmpty()) {
            return null;
        }
        Map.Entry<Price, LinkedBlockingQueue<Order>> bestBids = bids.firstEntry();
        return bestBids.getValue().isEmpty() ? null : bestBids.getValue();
    }

    public LinkedBlockingQueue<Order> getNationalBestOffers() {
        if (asks.isEmpty()) {
            return null;
        }
        Map.Entry<Price, LinkedBlockingQueue<Order>> bestAsks = asks.firstEntry();
        return bestAsks.getValue().isEmpty() ? null : bestAsks.getValue();
    }
    
    public Price getNationalBestBidPrice() {
        return bids.isEmpty() ? null : bids.firstKey();
    }
    
    public Price getNationalBestOfferPrice() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    // Getter methods
    
    public long getOrderBookId() {
        return getId();
    }

    public Asset getAsset() {
        return asset;
    }
    
    public Price getLastTradePrice() {
        return lastTradePrice;
    }
    
    public List<CompletedTrade> getTradeHistory() {
        return new ArrayList<>(tradeHistory);
    }
    
    public int getPendingOrderCount() {
        return orderQueue.size();
    }
    
    public int getStopOrderCount() {
        return buyStopQueue.size() + sellStopQueue.size() + 
               buyStopLimitQueue.size() + sellStopLimitQueue.size();
    }
}
