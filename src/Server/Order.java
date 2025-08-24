package Server;

public class Order extends Identifiable<Order> {
    /**
     * final fields as this is the Order object to be submitted to the OrderBook after client clicks CONFIRM
     */
    private final OrderType orderType;
    private final boolean isBid;
    private final int quantity;
    private final Asset asset;

    public Order(String clientId, OrderType ot, boolean iB, int shares, Asset assetAction) {
        super();  // calls Identifiable constructor to assign a unique id
        this.orderType = ot;
        this.isBid =iB;
        this.quantity = shares;
        this.asset = assetAction;
    }

    public long getOrderId(){
        return getId();
    }

    public Asset getAsset(){
        return asset;
    }

}
