package com.example.bloom_filter.service;

import com.example.bloom_filter.entity.Profile;
import com.example.bloom_filter.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for generating sample user data.
 * <p>
 * This service provides functionality to create sample users and their associated profiles
 * for testing, development, or demonstration purposes. It handles batch processing of user
 * creation and ensures that usernames are added to the Bloom filter for efficient lookups.
 */
@Service
public class UserGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(UserGenerationService.class);
    private static final int BATCH_SIZE = 10_000;

    private final UserNameBloomFilterService bloomFilterService;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructs a UserGenerationService with the required dependencies.
     *
     * @param bloomFilterService service for managing the username Bloom filter
     */
    public UserGenerationService(UserNameBloomFilterService bloomFilterService) {
        this.bloomFilterService = bloomFilterService;
    }

    /**
     * Generates a range of sample users and their associated profiles.
     * <p>
     * This method creates users with sequential usernames and email addresses,
     * along with randomized demographic information. It also creates a profile
     * for each user and adds the username to the Bloom filter for efficient lookups.
     * The method uses batch processing to optimize database operations.
     *
     * @param start the starting index for user generation
     * @param end the ending index for user generation (inclusive)
     */
    @Transactional
    public void generateUsers(long start, long end) {
        for (long i = start; i <= end; i++) {
            // Create User
            User user = new User();
            user.setUserName("user_" + i);
            user.setEmail("user_" + i + "@example.com");
            user.setFullName("User " + i);
            user.setAddress("Address " + i);
            user.setCity("City " + (i % 100));
            user.setState("State " + (i % 50));
            user.setCountry("Country " + (i % 10));
            user.setPhoneNumber("999999" + (1000 + (i % 9000)));
            user.setDateOfBirth(LocalDateTime.now().minusYears(20 + (i % 30)));
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());
            user.setBiography("This is a sample biography for user " + i);

            entityManager.persist(user);

            // Add the username to the Bloom filter
            bloomFilterService.addUserName(user.getUserName());

            // Create Profile and associate with User
            Profile profile = new Profile();
            profile.setFullName(user.getFullName());
            profile.setAddress(user.getAddress());
            profile.setUser(user); // set FK

            entityManager.persist(profile);

            if (i % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
                logger.info("Inserted {} users (and profiles)", i);
            }
        }
    }
}