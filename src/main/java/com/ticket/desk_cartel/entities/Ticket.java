package com.ticket.desk_cartel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Setter
@Getter
@Entity@Table(name = "ticket")
public class Ticket {

    @Id    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @Column
    private String title;

    @Column
    private String description;

    @OneToOne    @JoinColumn(name = "clientId")
    @JsonBackReference("account-sentTransactions")
    private User ticketOwner;

    @OneToOne
    @JoinColumn(name = "agentId")
    @JsonBackReference("account-sentTransactions")
    private Agent assignedTicket;

    @Column
    private int points;

    @Column
    private LocalDateTime date_started;

    @Column
    private LocalDateTime completion_date;

    @Column
    private Status status;

    @Column
    private Priority priority;

    @Column
    private String category;

}