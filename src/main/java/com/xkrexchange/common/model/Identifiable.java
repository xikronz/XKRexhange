package com.xkrexchange.common.model;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong; //threadsafe counter that atomically increments its value
import java.util.concurrent.ConcurrentHashMap; //threadsafe HashMap object that stores all idTypes

/** Manages all Identification (Ids) generation across classes i.e. Assets, Orders, Orderbook
 * Ensures Ids across assets can be duplicates but NOT within the same class
 */
public abstract class Identifiable<T> {

    private static final Map<Class<?>, AtomicLong> counters = new ConcurrentHashMap<>(); //aggreates all counters together
    private final long id; //assigns a unique id to each instance of Identifiable objects (note all Classes requiring an Id will extend Identifiable)

    protected Identifiable(){
        Class<?> cls = this.getClass();

        counters.putIfAbsent(cls, new AtomicLong(0)); //inserts a new pair counters.containsKey(cls) is false
        AtomicLong counter = counters.get(cls);
        this.id = counter.incrementAndGet();
    }

    public long getId(){
        return this.id;
    }
}
