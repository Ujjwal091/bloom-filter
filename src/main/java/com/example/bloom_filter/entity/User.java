package com.example.bloom_filter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a user in the system.
 * <p>
 * This entity stores core user information including authentication details,
 * personal information, and account status. It is used for user management,
 * authentication, and maintaining user state in the application.
 * </p>
 * The User entity has a one-to-one relationship with the Profile entity,
 * allowing for separation of core user data from extended profile information.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The username used for authentication.
     * This field is required and has a maximum length of 50 characters.
     */
    @Column(nullable = false, length = 50)
    private String userName;

    /**
     * The user's email address.
     * This field is required, must be unique, and has a maximum length of 100 characters.
     */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * The user's full name.
     * This field is required and has a maximum length of 100 characters.
     */
    @Column(nullable = false, length = 100)
    private String fullName;

    /**
     * The user's physical address.
     * This field is optional and has a maximum length of 255 characters.
     */
    @Column()
    private String address;

    /**
     * The city where the user resides.
     * This field is optional and has a maximum length of 100 characters.
     */
    @Column(length = 100)
    private String city;

    /**
     * The state or province where the user resides.
     * This field is optional and has a maximum length of 100 characters.
     */
    @Column(length = 100)
    private String state;

    /**
     * The country where the user resides.
     * This field is optional and has a maximum length of 20 characters.
     */
    @Column(length = 20)
    private String country;

    /**
     * The user's phone number.
     * This field is optional and has a maximum length of 15 characters.
     */
    @Column(length = 15)
    private String phoneNumber;

    /**
     * The user's date of birth.
     * This field is optional.
     */
    @Column
    private LocalDateTime dateOfBirth;

    /**
     * Flag indicating whether the user account is active.
     * This field is required.
     */
    @Column(nullable = false)
    private boolean isActive;

    /**
     * The date and time when the user account was created.
     * This field is required.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * The date and time of the user's last login.
     * This field is optional.
     */
    @Column
    private LocalDateTime lastLoginAt;

    /**
     * A brief description or personal statement provided by the user.
     * This field is optional and has a maximum length of 500 characters.
     */
    @Column(length = 500)
    private String biography;
}
