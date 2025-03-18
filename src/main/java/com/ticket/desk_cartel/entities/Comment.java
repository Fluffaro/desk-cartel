package com.ticket.desk_cartel.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private User author_id;


    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket_id;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private LocalDateTime comment_timestamp;

    @ManyToOne
    @JoinColumn(name = "reply_to_comment_id")
    private Comment replyToComment;


}
