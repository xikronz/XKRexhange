package Server;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/** Logic behind matching orders and keeping track of NBBO prices so that Pro-Rata Priority can be implemented when fulfilling orders
 * CLASS INVARIANT: Orderbooks must be Assigned to a UNIQUE asset object only
 */

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
