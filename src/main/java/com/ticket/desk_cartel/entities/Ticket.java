package com.ticket.desk_cartel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity@Table(name = "ticket")
public class Ticket {

    @Id    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ticketId;


    @Column    private String title;
    @Column    private String description;
    @OneToOne    @JoinColumn(name = "clientId")
    @JsonBackReference("account-sentTransactions")
    private User ticketOwners;

    @OneToOne    @JoinColumn(name = "agentId")
    @JsonBackReference("account-sentTransactions")
    private Agent assignedTickets;

}