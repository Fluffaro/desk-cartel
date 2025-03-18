package com.ticket.desk_cartel.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    
    @Column(length = 1000)
    private String message;
    
    private boolean isRead = false;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Reference to related ticket (if applicable)
    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;
}