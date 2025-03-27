package com.ticket.desk_cartel.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Data Transfer Object (DTO) for User.
 * Excludes sensitive fields like password, active status, and verification status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Date dateOfBirth;
    private String phoneNumber;
    private String address;
    private String profilePictureUrl;
    private String role;
    private LocalDateTime createdAt;
}
