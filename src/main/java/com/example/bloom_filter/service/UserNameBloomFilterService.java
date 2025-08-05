package com.example.bloom_filter.service;

import com.example.bloom_filter.config.BloomFilterConfig;
import com.example.bloom_filter.entity.User;
import com.example.bloom_filter.repository.UserRepository;
import com.example.bloom_filter.util.PersistableBloomFilter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service to manage a Bloom filter for username existence checks.
 * This implementation supports Redis-based persistence and batch processing for large datasets.
 * <p>
 * The Bloom filter is stored in Redis for fast access and sharing across multiple application instances.
 * This provides several advantages over file-based persistence:
 * <ul>
 *   <li>Faster access times for loading and saving the filter</li>
 *   <li>Ability to share the filter across multiple application instances</li>
 *   <li>Better support for containerized environments</li>
 *   <li>Automatic expiration and memory management via Redis</li>
 * </ul>
 * <p>
 * The service can be configured to save the filter to Redis after each username addition,
 * or only during initialization and shutdown.
 */
@Service
public class UserNameBloomFilterService {
    private static final Logger logger = LoggerFactory.getLogger(UserNameBloomFilterService.class);

    /**
     * Expected number of insertions (users) in the Bloom filter.
     * Used to calculate optimal size and hash functions.
     */
    @Value("${bloom.filter.expected.insertions:10000000}")
    private int expectedInsertions;

    /**
     * Desired false positive rate for the Bloom filter.
     * Lower values result in larger filter size but fewer false positives.
     */
    @Value("${bloom.filter.false.positive.rate:0.01}")
    private double falsePositiveRate;

    /**
     * Batch size for loading users from the database.
     * Smaller values use less memory but require more database queries.
     */
    @Value("${bloom.filter.batch.size:10000}")
    private int batchSize;

    /**
     * Redis key for storing the Bloom filter.
     * This key is used to store and retrieve the Bloom filter from Redis.
     */
    @Value("${bloom.filter.redis.key:bloom:filter:usernames}")
    private String redisKey;

    /**
     * Whether to rebuild the Bloom filter on startup even if a persisted version exists.
     */
    @Value("${bloom.filter.force.rebuild:false}")
    private boolean forceRebuild;

    /**
     * Whether to save the Bloom filter to Redis after each addition.
     * This can ensure data consistency across application restarts but may impact performance.
     */
    @Value("${bloom.filter.save.on.add:false}")
    private boolean saveOnAdd;

    /**
     * Whether to verify Redis operations by reading back the saved data.
     * This is useful for debugging but adds extra Redis calls.
     * Disabled by default in tests.
     */
    @Value("${bloom.filter.verify.redis:true}")
    private boolean verifyRedisOperations;

    /**
     * Whether Redis is enabled for Bloom filter persistence.
     * If set to false, Redis operations will be skipped and the filter will always be built from the database.
     */
    @Value("${bloom.filter.redis.enabled:true}")
    private boolean redisEnabled;

    /**
     * Redis URL for connection.
     * Used to extract host information for error messages.
     */
    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private PersistableBloomFilter<String> bloomFilter;

    public UserNameBloomFilterService(UserRepository userRepository, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Initialize the Bloom filter with all existing usernames.
     * This method first tries to load a persisted Bloom filter from Redis if available and enabled.
     * If Redis is disabled, not available, or if forceRebuild is true, it builds the filter from the database.
     *
     * @throws IllegalArgumentException if the configuration values are invalid
     */
    @PostConstruct
    public void initializeBloomFilter() {
        // Validate configuration values
        validateConfiguration();

        boolean shouldBuildFromDatabase;
        boolean shouldSaveToRedis = false;

        // Check if Redis is enabled
        if (!redisEnabled) {
            logger.info("Redis is disabled for Bloom filter persistence. Building filter from database.");
            shouldBuildFromDatabase = true;
        }
        // Try to load from Redis if enabled and not forced to rebuild
        else if (!forceRebuild) {
            logger.info("Attempting to load Bloom filter from Redis with key: {}", redisKey);
            try {
                Object cachedFilter = redisTemplate.opsForValue().get(redisKey);
                logger.debug("Retrieved object from Redis: {}",
                        cachedFilter != null ? cachedFilter.getClass().getName() : "null");

                if (cachedFilter == null) {
                    logger.info("No Bloom filter found in Redis, will build from database");
                    shouldBuildFromDatabase = true;
                    shouldSaveToRedis = true;
                } else if (cachedFilter instanceof PersistableBloomFilter) {
                    logger.info("Found valid Bloom filter in Redis");
                    bloomFilter = (PersistableBloomFilter<String>) cachedFilter;
                    logger.info("Bloom filter loaded successfully from Redis");
                    return;
                } else {
                    logger.warn("Found object in Redis but it's not a PersistableBloomFilter. Type: {}",
                            cachedFilter.getClass().getName());
                    shouldBuildFromDatabase = true;
                    shouldSaveToRedis = true;
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Connection refused")) {
                    logger.warn("Failed to connect to Redis: {}. Please check that Redis is running at the configured URL: {}. " +
                            "If Redis is intentionally not available, set bloom.filter.redis.enabled=false to disable Redis usage. " +
                            "Creating Bloom filter without Redis persistence.", errorMessage, getRedisHostInfo());
                } else {
                    logger.warn("Failed to load Bloom filter from Redis: {}. Creating Bloom filter without Redis persistence.",
                            errorMessage, e);
                }
                shouldBuildFromDatabase = true;
            }
        } else {
            logger.info("Force rebuild enabled, skipping Redis lookup");
            shouldBuildFromDatabase = true;
            shouldSaveToRedis = redisEnabled;
        }

        if (!shouldBuildFromDatabase) return;

        // Calculate optimal size and hash functions based on expected insertions and false positive rate
        BloomFilterConfig config = new BloomFilterConfig(expectedInsertions, falsePositiveRate);
        int optimalSize = config.optimalSize();
        int optimalHashCount = config.optimalHashCount();

        logger.info("Initializing Bloom filter with size: {}, hash functions: {}", optimalSize, optimalHashCount);

        // Create a new Bloom filter
        this.bloomFilter = new PersistableBloomFilter<>(optimalSize, optimalHashCount);

        // Build the Bloom filter from the database using batch processing
        buildBloomFilterFromDatabase();

        // Save the Bloom filter to Redis if needed
        if (shouldSaveToRedis) {
            saveBloomFilter();
        }
    }

    /**
     * Builds the Bloom filter from the database using batch processing.
     */
    private void buildBloomFilterFromDatabase() {
        logger.info("Building Bloom filter from database using batch processing");

        // Get total count of users for logging
        long totalUsers = userRepository.count();
        logger.info("Total users to process: {}", totalUsers);

        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            logger.info("Processing batch {}, size {}", page, batchSize);
            Page<User> userPage = userRepository.findAll(PageRequest.of(page, batchSize));

            for (User user : userPage.getContent()) {
                bloomFilter.add(user.getUserName());
            }

            hasMore = page < userPage.getTotalPages() - 1;
            page++;
        }

        logger.info("Bloom filter built successfully");
    }

    /**
     * Saves the Bloom filter to Redis if Redis is enabled.
     * If Redis is disabled, this method does nothing.
     */
    private void saveBloomFilter() {
        // Skip Redis operations if Redis is disabled
        if (!redisEnabled) {
            logger.debug("Redis is disabled, skipping save operation");
            return;
        }

        try {
            logger.info("Saving Bloom filter to Redis with key: {}", redisKey);
            logger.debug("Bloom filter state: {}, null check: {}",
                    bloomFilter != null ? "valid" : "invalid",
                    bloomFilter != null ? "not null" : "null");

            redisTemplate.opsForValue().set(redisKey, bloomFilter);
            logger.info("Bloom filter saved to Redis");

            // Verify the filter was saved by trying to retrieve it (only if verification is enabled)
            if (verifyRedisOperations) {
                Object cachedFilter = redisTemplate.opsForValue().get(redisKey);
                if (cachedFilter instanceof PersistableBloomFilter) {
                    logger.info("Successfully verified Bloom filter in Redis");
                } else {
                    logger.warn("Failed to verify Bloom filter in Redis. Retrieved object: {}",
                            cachedFilter != null ? cachedFilter.getClass().getName() : "null");
                }
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Connection refused")) {
                logger.error("Failed to connect to Redis at {}. Please check that Redis is running. " +
                                "If Redis is intentionally not available, set bloom.filter.redis.enabled=false to disable Redis usage.",
                        getRedisHostInfo());
            } else {
                logger.error("Failed to save Bloom filter to Redis: {}", errorMessage, e);
            }
        }
    }

    /**
     * Save the Bloom filter to Redis before the application shuts down if Redis is enabled.
     * If Redis is disabled, this method does nothing.
     */
    @PreDestroy
    public void saveOnShutdown() {
        if (!redisEnabled) {
            logger.info("Application shutting down, Redis is disabled, skipping save operation");
            return;
        }
        logger.info("Application shutting down, saving Bloom filter to Redis");
        saveBloomFilter();
    }

    /**
     * Validates that the configuration values are appropriate for the Bloom filter.
     *
     * @throws IllegalArgumentException if the configuration values are invalid
     */
    private void validateConfiguration() {
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException(
                    "Expected insertions must be positive, but was: " + expectedInsertions);
        }

        if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
            throw new IllegalArgumentException(
                    "False positive rate must be between 0 and 1, but was: " + falsePositiveRate);
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be positive, but was: " + batchSize);
        }
    }

    /**
     * Extracts host and port information from the Redis URL for error messages.
     *
     * @return A string containing the Redis host and port, or the full URL if parsing fails
     */
    private String getRedisHostInfo() {
        try {
            URI uri = new URI(redisUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            if (port == -1) {
                port = 6379; // Default Redis port
            }
            return host + ":" + port;
        } catch (URISyntaxException e) {
            logger.debug("Failed to parse Redis URL: {}", redisUrl, e);
            return redisUrl; // Return the full URL if parsing fails
        }
    }

    /**
     * Check if a username might exist using the Bloom filter.
     *
     * @param userName The username to check
     * @return true if the username might exist, false if it definitely doesn't exist
     */
    public boolean mightContainUserName(String userName) {
        return bloomFilter.mightContain(userName);
    }

    /**
     * Add a username to the Bloom filter.
     * This method also saves the updated filter to Redis if both Redis is enabled and saveOnAdd is enabled.
     *
     * @param userName The username to add
     */
    public void addUserName(String userName) {
        bloomFilter.add(userName);

        // Save the filter to Redis after each addition if Redis is enabled and configured to save on add
        if (redisEnabled && saveOnAdd) {
            logger.debug("Saving Bloom filter to Redis after adding username: {}", userName);
            saveBloomFilter();
        } else if (saveOnAdd) {
            logger.debug("Redis is disabled, skipping save operation after adding username: {}", userName);
        }
    }
}