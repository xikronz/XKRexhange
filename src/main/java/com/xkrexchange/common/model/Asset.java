package com.xkrexchange.common.model;

import com.xkrexchange.matching.OrderBook;

/**
 * Asset class that tracks trade-able asssets with unqiue ids. Each asset has its own dedicated OrderBook which is
 */

public class Asset extends Identifiable<Asset>{
    private String name;
    private String tick;
    private int currentPrice;
    private int sharesoutstanding;
    private int volume;
    private int marketCap;

    private OrderBook orderBook;

    public Asset (){
        super();
    }

    public long getAssetId(){
        return getId();
    }
}
