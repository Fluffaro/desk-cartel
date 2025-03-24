package com.ticket.desk_cartel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

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

    @Column
    private int notifCount;

    public int getNotifCount() {
        return notifCount;
    }

    public void setNotifCount(int notifCount) {
        this.notifCount = notifCount;
    }

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
    private String role = "CLIENT";


    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonBackReference
    private List<Comment> comments;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
