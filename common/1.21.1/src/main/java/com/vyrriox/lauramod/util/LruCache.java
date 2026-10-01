package com.vyrriox.lauramod.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * A small cache that keeps the entries used last: beyond its capacity the least recently used entry
 * is removed and handed to a callback (to release a texture, close a file...). Not thread safe.
 *
 * @author vyrriox
 */
public final class LruCache<K, V> {
    private final int capacity;
    private final Consumer<V> onEvict;
    private final LinkedHashMap<K, V> map = new LinkedHashMap<>(16, 0.75F, true);

    public LruCache(int capacity, Consumer<V> onEvict) {
        this.capacity = Math.max(1, capacity);
        this.onEvict = onEvict;
    }

    /** The value for the key, which becomes the most recently used one. */
    public V get(K key) {
        return map.get(key);
    }

    /** Stores a value and returns the one it replaces (which is not handed to the callback). */
    public V put(K key, V value) {
        V previous = map.put(key, value);
        Iterator<Map.Entry<K, V>> oldest = map.entrySet().iterator();
        while (map.size() > capacity && oldest.hasNext()) {
            V evicted = oldest.next().getValue();
            oldest.remove();
            onEvict.accept(evicted);
        }
        return previous;
    }

    /** Removes the entries the test accepts, without the callback. */
    public void removeIf(BiPredicate<K, V> test) {
        map.entrySet().removeIf(e -> test.test(e.getKey(), e.getValue()));
    }

    public int size() {
        return map.size();
    }

    /** Hands every value to the callback and empties the cache. */
    public void clear() {
        for (V value : map.values()) {
            onEvict.accept(value);
        }
        map.clear();
    }
}
