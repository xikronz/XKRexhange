package com.xkrexchange.matching;

import com.xkrexchange.common.model.Asset;
import com.xkrexchange.common.model.Order;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Central routing logic (doesn't actually implement any of the order matching logic) but pools together client side orders and routes them over into
 * the respective Asset's OrderBook
 *
 *  Stores all assets using a Map<String, Asset> were the String is the unqiue ticker of the asset
 */
public class MatchingEngine {

    // TODO: Add your matching engine implementation here
    // This will be called by the message queue consumer when orders arrive

}
