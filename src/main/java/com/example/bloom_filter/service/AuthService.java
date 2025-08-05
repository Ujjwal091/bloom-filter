package com.example.bloom_filter.service;

import com.example.bloom_filter.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Service responsible for authentication-related operations.
 * <p>
 * This service provides functionality for checking user existence and other
 * authentication-related operations. It uses a Bloom filter optimization to
 * efficiently check if usernames exist without always hitting the database.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserNameBloomFilterService bloomFilterService;

    /**
     * Constructs an AuthService with the required dependencies.
     *
     * @param userRepository     repository for accessing user data
     * @param bloomFilterService service for checking username existence in the Bloom filter
     */
    public AuthService(UserRepository userRepository, UserNameBloomFilterService bloomFilterService) {
        this.userRepository = userRepository;
        this.bloomFilterService = bloomFilterService;
    }

    /**
     * Checks if a username exists in the system.
     * <p>
     * This method uses a Bloom filter optimization to efficiently check username existence.
     * If the Bloom filter indicates the username definitely doesn't exist, the method returns
     * false without querying the database. If the Bloom filter indicates the username might
     * exist, the database is queried to verify.
     *
     * @param userName the username to check for existence
     * @return true if the username exists, false otherwise
     */
    public boolean userNameExists(String userName) {
        // First check the Bloom filter - if it says the username definitely doesn't exist,
        // we can return false without hitting the database
        if (!bloomFilterService.mightContainUserName(userName)) {
            return false;
        }

        // If the Bloom filter indicates the username might exist, verify with the database
        return userRepository.existsByUserName(userName);
    }
}