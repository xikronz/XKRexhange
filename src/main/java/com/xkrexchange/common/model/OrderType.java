package com.xkrexchange.common.model;

/** Enum Class for the types of client side order types
 * <p>LIMIT ORDER: fulfilled ONLY when OrderBook.getNBBOPrice.value >= Order.price.value</p>
 * <p>MARKET ORDER: fulfilled at market (NBBO) or above market price at {@code isBid} depending on quantity </p>
 * <p>STOP ORDER: becomes MARKET ORDER as soon as {@code triggerPrice} is hit on the bid/ask. Guarantees execution but at a price NO WORSE than {@code triggerPrice} </p>
 * <p>STOP_LIMIT: becomes a LIMIT ORDER at {@code price} == {@code triggerPrice} as soon as {@code triggerPrice} is penetrated in the OrderBook. At risk of unfulfillment if the Market moves too quickly </p>
 */

public enum OrderType {
    LIMIT,
    MARKET,
    STOP,
    STOP_LIMIT;
}
