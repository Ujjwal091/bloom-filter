package com.example.bloom_filter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a user profile in the system.
 * <p>
 * This entity stores simplified profile information associated with a User entity.
 * It maintains a one-to-one relationship with the User entity, allowing for separation
 * of core user authentication data from profile information.
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
public class Profile {

    /**
     * Unique identifier for the profile.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The full name of the user associated with this profile.
     * This field is required.
     */
    @Column(nullable = false)
    private String fullName;

    /**
     * The physical address of the user.
     * This field is optional.
     */
    @Column
    private String address;

    /**
     * The User entity associated with this profile.
     * Each profile is linked to exactly one user through a one-to-one relationship.
     * The relationship is mapped using the user_id foreign key.
     */
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}
