package com.example.bloom_filter.config;

/**
 * Configuration class for calculating optimal Bloom filter parameters.
 * <p>
 * A Bloom filter is a space-efficient probabilistic data structure used to test whether an element
 * is a member of a set. False positives are possible, but false negatives are not. This class
 * provides methods to calculate the optimal size of the bit array and the optimal number of hash
 * functions based on the expected number of insertions and the desired false positive rate.
 * <p>
 * The mathematical formulas used in this class are derived from the probability theory behind
 * Bloom filters and are widely accepted as the standard approach for optimizing Bloom filter
 * parameters.
 * <p>
 * Usage example:
 * <pre>
 * BloomFilterConfig config = new BloomFilterConfig(10_000_000, 0.01);
 * int optimalSize = config.optimalSize();
 * int optimalHashCount = config.optimalHashCount();
 * BloomFilter&lt;String&gt; filter = new BloomFilter&lt;&gt;(optimalSize, optimalHashCount);
 * </pre>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bloom_filter">Bloom filter (Wikipedia)</a>
 */
public record BloomFilterConfig(
        int expectedInsertions,
        double falsePositiveRate
) {

    /**
     * Calculates the optimal size of the bit array for the Bloom filter.
     * <p>
     * The formula used is: m = -n * ln(p) / (ln(2)^2)
     * <p>
     * Where:
     * <ul>
     *   <li>m is the size of the bit array</li>
     *   <li>n is the expected number of insertions</li>
     *   <li>p is the false positive rate</li>
     * </ul>
     * <p>
     * This formula minimizes the false positive rate for a given number of elements
     * and a fixed-size bit array.
     *
     * @return the optimal size of the bit array as an integer (rounded up)
     */
    public int optimalSize() {
        return (int) Math.ceil(-(expectedInsertions * Math.log(falsePositiveRate)) / (Math.pow(Math.log(2), 2)));
    }

    /**
     * Calculates the optimal number of hash functions for the Bloom filter.
     * <p>
     * The formula used is: k = (m/n) * ln(2)
     * <p>
     * Where:
     * <ul>
     *   <li>k is the number of hash functions</li>
     *   <li>m is the size of the bit array (calculated by {@link #optimalSize()})</li>
     *   <li>n is the expected number of insertions</li>
     * </ul>
     * <p>
     * This formula optimizes the number of hash functions to minimize the false positive rate
     * for a given bit array size and number of elements.
     *
     * @return the optimal number of hash functions as an integer (rounded to the nearest integer)
     */
    public int optimalHashCount() {
        return (int) Math.round((optimalSize() / (double) expectedInsertions) * Math.log(2));
    }
}
