package com.ticket.desk_cartel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Setter
@Getter
@Entity@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @Column
    private String title;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @JsonBackReference("account-sentTransactions")
    private User ticketOwner;

    @ManyToOne
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
    private LocalDateTime expected_completion_date;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @ManyToOne
    @JoinColumn(name ="category_id")
    @JsonManagedReference
    private Category category;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonBackReference
    private List<Comment> comments;


}