package Server;

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
