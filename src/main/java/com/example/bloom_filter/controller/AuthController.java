package com.example.bloom_filter.controller;

import com.example.bloom_filter.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication-related operations.
 * <p>
 * This controller provides endpoints for checking user existence and other authentication
 * operations. It uses Bloom filter optimization through the AuthService to efficiently
 * check if usernames exist without always hitting the database.
 */
@RestController
public class AuthController {

    private final AuthService authService;

    /**
     * Constructs an AuthController with the required AuthService dependency.
     *
     * @param authService the service that handles authentication operations
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Checks if a username exists in the system.
     * <p>
     * This endpoint uses a Bloom filter optimization to efficiently check username existence.
     * If the Bloom filter indicates the username definitely doesn't exist, the method returns
     * false without querying the database. If the Bloom filter indicates the username might
     * exist, the database is queried to verify.
     *
     * @param userName the username to check for existence
     * @return true if the username exists, false otherwise
     */
    @GetMapping("/users/exists")
    public boolean userNameExists(@RequestParam String userName) {
        return authService.userNameExists(userName);
    }
}
