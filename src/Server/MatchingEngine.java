package Server;

import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Central routing logic (doesn't actually implement any of the order matching logic) but pools together client side orders and routes them over into
 * the respective Asset's OrderBook
 *
 *  Stores all assets using a Map<String, Asset> were the String is the unqiue ticker of the asset
 */
public class MatchingEngine {

}
