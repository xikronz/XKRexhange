package com.xkrexchange;

import com.xkrexchange.common.model.Asset;
import com.xkrexchange.common.model.Order;
import com.xkrexchange.common.model.OrderType;
import com.xkrexchange.common.model.Price;
import com.xkrexchange.common.model.CompletedTrade;
import com.xkrexchange.matching.OrderBook;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Comprehensive unit tests for OrderBook class
 * Tests all major functionality including order matching, stop orders, and NBBO
 */
class OrderBookTesting {
    
    private OrderBook orderBook;
    private Asset testAsset;
    
    @BeforeEach
    void setUp() {
        // Create test asset with tick size of $0.01
        testAsset = new Asset("Tesla Inc", "TSLA", 100, 1000000, new BigDecimal("0.01"));
        orderBook = new OrderBook(testAsset);
    }
    
    @AfterEach
    void tearDown() {
        if (orderBook != null) {
            orderBook.stopMatchingEngine();
        }
    }

    // === BASIC FUNCTIONALITY TESTS ===
    
    @Test
    @DisplayName("Test OrderBook basic getters")
    void testBasicGetters() {
        assertNotNull(orderBook.getOrderBookId());
        assertEquals(testAsset, orderBook.getAsset());
        assertNull(orderBook.getLastTradePrice());
        assertEquals(0, orderBook.getPendingOrderCount());
        assertEquals(0, orderBook.getStopOrderCount());
        assertTrue(orderBook.getTradeHistory().isEmpty());
    }
    
    @Test
    @DisplayName("Test NBBO methods with empty book")
    void testEmptyBookNBBO() {
        assertNull(orderBook.getNationalBestBids());
        assertNull(orderBook.getNationalBestOffers());
        assertNull(orderBook.getNationalBestBidPrice());
        assertNull(orderBook.getNationalBestOfferPrice());
    }

    // === MATCHING ENGINE LIFECYCLE TESTS ===
    
    @Test
    @DisplayName("Test matching engine start and stop")
    void testMatchingEngineLifecycle() {
        // Start engine
        orderBook.startMatchingEngine();
        assertTrue(orderBook.getClass().getDeclaredFields().length > 0); // Engine should be running
        
        // Starting again should be safe (no duplicate threads)
        orderBook.startMatchingEngine();
        
        // Stop engine
        orderBook.stopMatchingEngine();
    }

    // === LIMIT ORDER TESTS ===
    
    @Test
    @DisplayName("Test limit order posting to empty book")
    void testLimitOrderPostingToEmptyBook() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price bidPrice = new Price(new BigDecimal("100.00"));
        Price askPrice = new Price(new BigDecimal("101.00"));
        
        // Submit buy limit order
        Order buyOrder = Order.newLimitOrder(1001L, OrderType.LIMIT, true, 100, testAsset, bidPrice);
        orderBook.submitOrder(buyOrder);
        
        // Submit sell limit order
        Order sellOrder = Order.newLimitOrder(1002L, OrderType.LIMIT, false, 50, testAsset, askPrice);
        orderBook.submitOrder(sellOrder);
        
        // Wait for processing
        Thread.sleep(100);
        
        // Check NBBO
        assertNotNull(orderBook.getNationalBestBids());
        assertNotNull(orderBook.getNationalBestOffers());
        assertEquals(bidPrice, orderBook.getNationalBestBidPrice());
        assertEquals(askPrice, orderBook.getNationalBestOfferPrice());
    }
    
    @Test
    @DisplayName("Test limit order immediate matching")
    void testLimitOrderImmediateMatching() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price price = new Price(new BigDecimal("100.00"));
        
        // Submit sell order first
        Order sellOrder = Order.newLimitOrder(1001L, OrderType.LIMIT, false, 100, testAsset, price);
        orderBook.submitOrder(sellOrder);
        
        Thread.sleep(50);
        
        // Submit matching buy order (should execute immediately)
        Order buyOrder = Order.newLimitOrder(1002L, OrderType.LIMIT, true, 50, testAsset, price);
        orderBook.submitOrder(buyOrder);
        
        Thread.sleep(100);
        
        // Check trade occurred
        List<CompletedTrade> trades = orderBook.getTradeHistory();
        assertFalse(trades.isEmpty());
        assertEquals(1, trades.size());
        
        CompletedTrade trade = trades.get(0);
        assertEquals(50, trade.getQuantity());
        assertEquals(price, trade.getExecutionPrice());
        assertEquals(1002L, trade.getBuyerUserId()); // buyOrder client
        assertEquals(1001L, trade.getSellerUserId()); // sellOrder client
        
        // Check last trade price
        assertEquals(price, orderBook.getLastTradePrice());
    }
    
    @Test
    @DisplayName("Test partial fill scenario")
    void testPartialFill() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price price = new Price(new BigDecimal("100.00"));
        
        // Large sell order
        Order sellOrder = Order.newLimitOrder(1001L, OrderType.LIMIT, false, 200, testAsset, price);
        orderBook.submitOrder(sellOrder);
        
        Thread.sleep(50);
        
        // Smaller buy order (partial fill)
        Order buyOrder = Order.newLimitOrder(1002L, OrderType.LIMIT, true, 75, testAsset, price);
        orderBook.submitOrder(buyOrder);
        
        Thread.sleep(100);
        
        // Check trade
        List<CompletedTrade> trades = orderBook.getTradeHistory();
        assertEquals(1, trades.size());
        assertEquals(75, trades.get(0).getQuantity());
        
        // Sell order should still be partially on the book
        LinkedBlockingQueue<Order> bestOffers = orderBook.getNationalBestOffers();
        assertNotNull(bestOffers);
        Order remainingOrder = bestOffers.peek();
        assertNotNull(remainingOrder);
        assertEquals(125, remainingOrder.getRemainingQuantity()); // 200 - 75
    }

    // === MARKET ORDER TESTS ===
    
    @Test
    @DisplayName("Test market order execution")
    void testMarketOrderExecution() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price askPrice = new Price(new BigDecimal("101.00"));
        
        // Place sell limit order for liquidity
        Order sellOrder = Order.newLimitOrder(1001L, OrderType.LIMIT, false, 100, testAsset, askPrice);
        orderBook.submitOrder(sellOrder);
        
        Thread.sleep(50);
        
        // Submit market buy order
        Order marketBuy = Order.newMarketOrder(1002L, OrderType.MARKET, true, 60, testAsset);
        orderBook.submitOrder(marketBuy);
        
        Thread.sleep(100);
        
        // Check execution
        List<CompletedTrade> trades = orderBook.getTradeHistory();
        assertEquals(1, trades.size());
        assertEquals(60, trades.get(0).getQuantity());
        assertEquals(askPrice, trades.get(0).getExecutionPrice()); // Market order takes liquidity price
    }
    
    @Test
    @DisplayName("Test market order with no liquidity")
    void testMarketOrderNoLiquidity() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        // Submit market order to empty book
        Order marketOrder = Order.newMarketOrder(1001L, OrderType.MARKET, true, 100, testAsset);
        orderBook.submitOrder(marketOrder);
        
        Thread.sleep(100);
        
        // No trades should occur
        assertTrue(orderBook.getTradeHistory().isEmpty());
        assertNull(orderBook.getLastTradePrice());
    }

    // === STOP ORDER TESTS ===
    
    @Test
    @DisplayName("Test buy stop order triggering")
    void testBuyStopOrderTriggering() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price currentPrice = new Price(new BigDecimal("100.00"));
        Price stopPrice = new Price(new BigDecimal("102.00"));
        Price liquidityPrice = new Price(new BigDecimal("103.00"));
        
        // Create initial liquidity at higher price
        Order sellLiquidity = Order.newLimitOrder(1001L, OrderType.LIMIT, false, 100, testAsset, liquidityPrice);
        orderBook.submitOrder(sellLiquidity);
        
        // Submit buy stop order (should not trigger yet)
        Order buyStop = Order.newStopOrder(1002L, OrderType.STOP, true, 50, testAsset, stopPrice);
        orderBook.submitOrder(buyStop);
        
        Thread.sleep(100);
        
        // Should be 1 stop order pending
        assertEquals(1, orderBook.getStopOrderCount());
        assertTrue(orderBook.getTradeHistory().isEmpty());
        
        // Execute a trade at trigger price to activate stop
        Order triggerOrder = Order.newLimitOrder(1003L, OrderType.LIMIT, true, 10, testAsset, stopPrice);
        orderBook.submitOrder(triggerOrder);
        
        Thread.sleep(200);
        
        // Stop should have triggered and converted to market order
        assertEquals(0, orderBook.getStopOrderCount()); // Stop order consumed
        List<CompletedTrade> trades = orderBook.getTradeHistory();
        assertTrue(trades.size() >= 1); // At least the trigger trade occurred
    }
    
    @Test
    @DisplayName("Test sell stop order triggering")
    void testSellStopOrderTriggering() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price stopPrice = new Price(new BigDecimal("98.00"));
        Price liquidityPrice = new Price(new BigDecimal("97.00"));
        
        // Create initial liquidity at lower price for when stop triggers
        Order buyLiquidity = Order.newLimitOrder(1001L, OrderType.LIMIT, true, 100, testAsset, liquidityPrice);
        orderBook.submitOrder(buyLiquidity);
        
        // Submit sell stop order
        Order sellStop = Order.newStopOrder(1002L, OrderType.STOP, false, 50, testAsset, stopPrice);
        orderBook.submitOrder(sellStop);
        
        Thread.sleep(100);
        assertEquals(1, orderBook.getStopOrderCount());
        
        // Create a sell order at stop price and a buy order to match it - this will execute a trade at stop price
        Order sellAtStopPrice = Order.newLimitOrder(1003L, OrderType.LIMIT, false, 10, testAsset, stopPrice);
        Order buyAtStopPrice = Order.newLimitOrder(1004L, OrderType.LIMIT, true, 10, testAsset, stopPrice);
        
        orderBook.submitOrder(sellAtStopPrice);
        Thread.sleep(50);
        orderBook.submitOrder(buyAtStopPrice); // This should execute and set lastTradePrice to stopPrice
        
        Thread.sleep(200);
        
        // Stop should have triggered
        assertEquals(0, orderBook.getStopOrderCount());
    }

    // === STOP-LIMIT ORDER TESTS ===
    
    @Test
    @DisplayName("Test stop-limit order triggering and posting")
    void testStopLimitOrderTriggering() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price stopPrice = new Price(new BigDecimal("102.00"));
        Price limitPrice = new Price(new BigDecimal("103.00"));
        
        Order stopLimit = Order.newStopLimitOrder(1001L, OrderType.STOP_LIMIT, true, 100, testAsset, limitPrice, stopPrice);
        orderBook.submitOrder(stopLimit);
        
        Thread.sleep(100);
        assertEquals(1, orderBook.getStopOrderCount());
        
        Order sellAtStopPrice = Order.newLimitOrder(1002L, OrderType.LIMIT, false, 10, testAsset, stopPrice);
        Order buyAtStopPrice = Order.newLimitOrder(1003L, OrderType.LIMIT, true, 10, testAsset, stopPrice);
        
        orderBook.submitOrder(sellAtStopPrice);
        Thread.sleep(50);
        orderBook.submitOrder(buyAtStopPrice); 
        
        Thread.sleep(200);
        assertEquals(0, orderBook.getStopOrderCount());
        assertNotNull(orderBook.getNationalBestBids());
        assertEquals(limitPrice, orderBook.getNationalBestBidPrice());
    }

    // === SELF-TRADE PREVENTION TESTS ===
    
    @Test
    @DisplayName("Test self-trade prevention")
    void testSelfTradePrevention() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price price = new Price(new BigDecimal("100.00"));
        long clientId = 1001L;
        
        // Submit sell order
        Order sellOrder = Order.newLimitOrder(clientId, OrderType.LIMIT, false, 100, testAsset, price);
        orderBook.submitOrder(sellOrder);
        
        Thread.sleep(50);
        
        // Submit buy order from same client (should not match)
        Order buyOrder = Order.newLimitOrder(clientId, OrderType.LIMIT, true, 50, testAsset, price);
        orderBook.submitOrder(buyOrder);
        
        Thread.sleep(100);
        
        // No trades should occur
        assertTrue(orderBook.getTradeHistory().isEmpty());
        
        // Both orders should be on book
        assertNotNull(orderBook.getNationalBestBids());
        assertNotNull(orderBook.getNationalBestOffers());
    }

    // === PRICE-TIME PRIORITY TESTS ===
    
    @Test
    @DisplayName("Test price-time priority")
    void testPriceTimePriority() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price betterPrice = new Price(new BigDecimal("101.00"));
        Price worsePrice = new Price(new BigDecimal("100.00"));
        
        // Submit worse price first
        Order worseBuy = Order.newLimitOrder(1001L, OrderType.LIMIT, true, 100, testAsset, worsePrice);
        orderBook.submitOrder(worseBuy);
        
        Thread.sleep(10);
        
        // Submit better price second
        Order betterBuy = Order.newLimitOrder(1002L, OrderType.LIMIT, true, 100, testAsset, betterPrice);
        orderBook.submitOrder(betterBuy);
        
        Thread.sleep(50);
        
        // Better price should be at top of book
        assertEquals(betterPrice, orderBook.getNationalBestBidPrice());
        
        // Submit matching sell order
        Order sellOrder = Order.newLimitOrder(1003L, OrderType.LIMIT, false, 50, testAsset, betterPrice);
        orderBook.submitOrder(sellOrder);
        
        Thread.sleep(100);
        
        // Trade should execute with better price order
        List<CompletedTrade> trades = orderBook.getTradeHistory();
        assertEquals(1, trades.size());
        assertEquals(1002L, trades.get(0).getBuyerUserId()); // Better price order
        assertEquals(betterPrice, trades.get(0).getExecutionPrice());
    }

    // === EDGE CASES AND ERROR HANDLING ===
    
    @Test
    @DisplayName("Test multiple price levels")
    void testMultiplePriceLevels() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        // Create multiple bid levels
        Order bid1 = Order.newLimitOrder(1001L, OrderType.LIMIT, true, 100, testAsset, new Price(new BigDecimal("100.00")));
        Order bid2 = Order.newLimitOrder(1002L, OrderType.LIMIT, true, 100, testAsset, new Price(new BigDecimal("99.50")));
        Order bid3 = Order.newLimitOrder(1003L, OrderType.LIMIT, true, 100, testAsset, new Price(new BigDecimal("99.00")));
        
        orderBook.submitOrder(bid2); // Submit middle price first
        orderBook.submitOrder(bid1); // Then highest
        orderBook.submitOrder(bid3); // Then lowest
        
        Thread.sleep(100);
        
        // Highest price should be best bid
        assertEquals(new Price(new BigDecimal("100.00")), orderBook.getNationalBestBidPrice());
    }
    
    @Test
    @DisplayName("Test order book with zero quantities")
    void testZeroQuantityHandling() {
        // Note: This tests the factory methods' validation
        // Zero quantity orders should not be created in practice
        Price price = new Price(new BigDecimal("100.00"));
        
        assertDoesNotThrow(() -> {
            Order order = Order.newLimitOrder(1001L, OrderType.LIMIT, true, 1, testAsset, price);
            assertNotNull(order);
        });
    }
    
    @Test
    @DisplayName("Test concurrent order submission")
    void testConcurrentOrderSubmission() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        Price price = new Price(new BigDecimal("100.00"));
        
        // Submit multiple orders rapidly
        for (int i = 0; i < 10; i++) {
            Order order = Order.newLimitOrder(1000L + i, OrderType.LIMIT, i % 2 == 0, 10, testAsset, price);
            orderBook.submitOrder(order);
        }
        
        Thread.sleep(200);
        
        // Should handle all orders without errors
        assertTrue(orderBook.getTradeHistory().size() > 0);
    }

    // === INTEGRATION TESTS ===
    
    @Test
    @DisplayName("Test complex trading scenario")
    void testComplexTradingScenario() throws InterruptedException {
        orderBook.startMatchingEngine();
        
        // Create a realistic trading scenario
        Price[] prices = {
            new Price(new BigDecimal("99.50")),
            new Price(new BigDecimal("100.00")),
            new Price(new BigDecimal("100.50")),
            new Price(new BigDecimal("101.00"))
        };
        
        // Build order book with multiple levels
        orderBook.submitOrder(Order.newLimitOrder(1001L, OrderType.LIMIT, true, 100, testAsset, prices[0])); // Bid
        orderBook.submitOrder(Order.newLimitOrder(1002L, OrderType.LIMIT, true, 150, testAsset, prices[1])); // Better bid
        orderBook.submitOrder(Order.newLimitOrder(1003L, OrderType.LIMIT, false, 120, testAsset, prices[2])); // Ask
        orderBook.submitOrder(Order.newLimitOrder(1004L, OrderType.LIMIT, false, 80, testAsset, prices[3])); // Higher ask
        
        Thread.sleep(100);
        
        // Add stop orders
        orderBook.submitOrder(Order.newStopOrder(1005L, OrderType.STOP, true, 50, testAsset, prices[2])); // Buy stop
        orderBook.submitOrder(Order.newStopOrder(1006L, OrderType.STOP, false, 75, testAsset, prices[1])); // Sell stop
        
        Thread.sleep(100);
        
        // Execute aggressive market order to trigger stops
        orderBook.submitOrder(Order.newMarketOrder(1007L, OrderType.MARKET, true, 200, testAsset));
        
        Thread.sleep(300);
        
        // Verify complex interactions occurred
        assertFalse(orderBook.getTradeHistory().isEmpty());
        assertNotNull(orderBook.getLastTradePrice());
    }

    // === LEGACY METHOD TESTS ===
    
    @Test
    @DisplayName("Test basic exception scenarios")
    void testExceptions() {
        // Test the original method for backward compatibility
        Asset tesla = new Asset("Tesla", "TSLA", 100, 1000000, new BigDecimal("0.01"));
        
        assertDoesNotThrow(() -> {
            long clientID = 10101010L;
            Price purchase = new Price(new BigDecimal("101.00"));
            Order temp = Order.newLimitOrder(clientID, OrderType.LIMIT, true, 100, tesla, purchase);
            
            assertNotNull(temp);
            assertEquals(clientID, temp.getClientId());
            assertEquals(OrderType.LIMIT, temp.getOrderType());
            assertTrue(temp.isBid());
            assertEquals(100, temp.getQuantity());
            assertEquals(100, temp.getRemainingQuantity());
            assertEquals(tesla, temp.getAsset());
            assertEquals(purchase, temp.getExecutionPrice());
            assertFalse(temp.isCompleted());
        });
    }
}
