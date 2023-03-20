//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

/**
 * Data structure for key/value pairs, where the value for each key is based on
 * an underlying compute function. The cache is lazy: values are only computed
 * when first requested. If the cache capacity has been exceeded, the oldest
 * values are removed. Note that "oldest" means the values that were least
 * recently *calculated*, not the values that were least recently *accessed*.
 */
public class Cache<K, V> {

    private Function<K, V> computeFunction;
    private Map<K, V> contents;
    private Deque<K> keyOrder;
    private int capacity;

    private Cache(Function<K, V> computeFunction, int capacity) {
        this.computeFunction = computeFunction;
        this.contents = new HashMap<>();
        this.keyOrder = new LinkedList<>();
        this.capacity = capacity;
    }

    /**
     * Returns the value mapped to the specified key. If the key/value pair is
     * currently present in the cache, the cached value will be returned.
     * Otherwise, the underlying function is used to compute the value, cache
     * it, then return it.
     */
    public V get(K key) {
        if (contents.containsKey(key)) {
            return contents.get(key);
        } else {
            V value = computeFunction.apply(key);
            contents.put(key, value);
            keyOrder.add(key);

            if (keyOrder.size() > capacity) {
                K evicted = keyOrder.removeFirst();
                contents.remove(evicted);
            }

            return value;
        }
    }

    /**
     * Precomputes the value for the specified key, so the cached value is used
     * when the key/value pair is retrieved at a later time.
     */
    public void precompute(K key) {
        get(key);
    }

    /**
     * Precomputes the values for the specified keys, so the cached values are
     * used when the key/value pairs are retrieved at a later time.
     */
    public void precompute(Iterable<K> keys) {
        for (K key : keys) {
            precompute(key);
        }
    }

    /**
     * Forgets a cached key/value pairs in this cache. If the key/value pair
     * was not yet computed this method does nothing.
     */
    public void forget(K key) {
        contents.remove(key);
        keyOrder.remove(key);
    }

    /**
     * Forgets all cached key/value pairs in this cache.
     */
    public void forgetAll() {
        contents.clear();
        keyOrder.clear();
    }

    /**
     * Returns true if the value for the specified key is currently cached, and
     * false if the value still needs to be computed.
     */
    protected boolean isCached(K key) {
        return contents.containsKey(key);
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Creates a lookup table based on the specified function. The lookup table
     * will have unlimited capacity, cached values will never be removed.
     */
    public static <K, V> Cache<K, V> from(Function<K, V> computeFunction) {
        return from(computeFunction, Integer.MAX_VALUE);
    }

    /**
     * Creates a lookup table based on the specified function. The lookup table
     * will have limited capacity. If the number of key/value pairs exceeds the
     * capacity, the oldest values will be removed.
     */
    public static <K, V> Cache<K, V> from(Function<K, V> computeFunction, int capacity) {
        return new Cache<>(computeFunction, capacity);
    }
}
