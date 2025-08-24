package Server;
import java.util.HashMap;
import java.util.PriorityQueue;

public class OrderBook extends Identifiable<OrderBook>{
    private HashMap<Float, PriorityQueue<Order>> bid = new HashMap<>();
    private HashMap<Float, PriorityQueue<Order>> ask = new HashMap<>();

    public OrderBook(){
        super();
    }

    public long getOrderBookId(){
        return getId();
    }
}
