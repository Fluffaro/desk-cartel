package com.ticket.desk_cartel.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Entity representing a user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;

    // Use LocalDate for date of birth.
    private Date dateOfBirth;

    private String phoneNumber;

    private String address;

    private String profilePictureUrl;

    private boolean isActive = true;

    private boolean isVerified = false;

    // Store a single role directly in this column. Default is "USER".
    @Column(nullable = false)
    private String role = "USER";

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
