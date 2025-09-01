package com.xkrexchange.common.model;

import com.xkrexchange.matching.OrderBook;
import java.math.BigDecimal;

/**
 * Asset class that tracks trade-able asssets with unqiue ids. Each asset has its own dedicated OrderBook which is
 */

public class Asset extends Identifiable<Asset>{
    private String name;
    private String ticker;
    private int currentPrice;
    private int sharesOutstanding;
    private int volume;
    private int marketCap;

    private OrderBook orderBook;
    private BigDecimal tick; 

    public Asset (String corporateName, String t, int initialPublicOffering, int sharesOut){
        super();
        orderBook = new OrderBook(this);

        name = corporateName; 
        ticker = t; 
        currentPrice = initialPublicOffering; 
        sharesOutstanding = sharesOut; 
        marketCap = sharesOutstanding*marketCap;  
    }

    public Asset(String n, String t, int initialPublicOffering, int sharesOut, BigDecimal tickIncrement){
        tick = tickIncrement; 
        orderBook = new OrderBook(this);

        name = n; 
        ticker = t; 
        currentPrice = initialPublicOffering; 
        sharesOutstanding = sharesOut;
        marketCap = sharesOutstanding*initialPublicOffering;  
    }

    public OrderBook getOrderBook(){
        return orderBook; 
    }


    public long getAssetId(){
        return getId();
    }
    
    public BigDecimal getTickSize() {
        return tick;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return this.name; 
    }
}
