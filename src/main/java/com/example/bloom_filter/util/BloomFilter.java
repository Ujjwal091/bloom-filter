package com.example.bloom_filter.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.BitSet;
import java.util.function.Function;

/**
 * A simple Bloom filter implementation for checking if an element might be in a set.
 * False positives are possible, but false negatives are not.
 * <p>
 * This class implements Serializable to support caching in Redis and other distributed caches.
 */
public class BloomFilter<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final BitSet bitSet;
    private final int size;
    private final int numHashFunctions;
    private transient Function<T, Integer>[] hashFunctions;

    /**
     * Creates a new Bloom filter.
     *
     * @param size             The size of the bit array
     * @param numHashFunctions The number of hash functions to use
     */
    @SuppressWarnings("unchecked")
    public BloomFilter(int size, int numHashFunctions) {
        this.size = size;
        this.numHashFunctions = numHashFunctions;
        this.bitSet = new BitSet(size);
        this.hashFunctions = new Function[numHashFunctions];

        // Create hash functions
        for (int i = 0; i < numHashFunctions; i++) {
            final int seed = i;
            hashFunctions[i] = element -> {
                int hash = element.hashCode();
                hash = (hash ^ seed) * 0x9E3779B9; // Mix with a constant
                hash = Math.abs(hash) % size;
                return hash;
            };
        }
    }

    /**
     * Adds an element to the Bloom filter.
     *
     * @param element The element to add
     */
    public void add(T element) {
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hashFunctions[i].apply(element);
            bitSet.set(hash);
        }
    }

    /**
     * Checks if an element might be in the set.
     *
     * @param element The element to check
     * @return true if the element might be in the set, false if it is definitely not in the set
     */
    public boolean mightContain(T element) {
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hashFunctions[i].apply(element);
            if (!bitSet.get(hash)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clears the Bloom filter.
     */
    public void clear() {
        bitSet.clear();
    }
    
    /**
     * Custom deserialization method to reconstruct the hash functions after deserialization.
     * This is needed because the hash functions are marked as transient.
     *
     * @param in The object input stream
     * @throws IOException If an I/O error occurs
     * @throws ClassNotFoundException If the class of a serialized object cannot be found
     */
    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        // Read the non-transient fields
        in.defaultReadObject();
        
        // Reconstruct the hash functions
        this.hashFunctions = new Function[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            final int seed = i;
            hashFunctions[i] = element -> {
                int hash = element.hashCode();
                hash = (hash ^ seed) * 0x9E3779B9; // Mix with a constant
                hash = Math.abs(hash) % size;
                return hash;
            };
        }
    }
}