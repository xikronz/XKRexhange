package com.xkrexchange;

import com.xkrexchange.common.model.Order;
import com.xkrexchange.common.model.*; 
import com.xkrexchange.matching.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.System;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTesting {
    private Random random;
    private OrderBook orderBook; 

    public void testExceptions(){
        Asset tesla = new Asset("Tesla", "TSLA", 100, 10000); 

        orderBook = tesla.getOrderBook(); 
        

        long clientID = 10101010; 

        Price purcahse = new Price(new BigDecimal("101.00")); 

        Order temp = Order.newLimitOrder(clientID, OrderType.LIMIT, true, 100, tesla, purcahse); 

        java.lang.System.out.println(temp);

    }

}
