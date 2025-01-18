package org.poo.Components;

/**
 * A simple generic class to hold a pair of values.
 *
 * @param <K> the type of the first value
 * @param <V> the type of the second value
 */
public final class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * Constructs a new Pair with the given key and value.
     *
     * @param key   the key
     * @param value the value
     */
    public Pair(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the key of this pair.
     *
     * @return the key
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the value of this pair.
     *
     * @return the value
     */
    public V getValue() {
        return value;
    }
}
