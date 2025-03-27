package com.ticket.desk_cartel.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages") // Explicit table name
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false) // Ensure content is always stored
    private String messageContent;

    @Column(nullable = false, updatable = false) // Prevent updates to timestamp
    private LocalDateTime timestamp;
    
    @Column(name = "ticket_id")
    private String ticketId; // To associate messages with specific tickets
    
    @Column(name = "client_message_id")
    private String clientMessageId; // For deduplication with the frontend

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
