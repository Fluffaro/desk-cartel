package com.ticket.desk_cartel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int notificationId;

    @Column(nullable = true)
    private String title;

    @Column(nullable = true)
    private String description;

    @ManyToOne
    @JoinColumn(name = "t_Id", nullable = true)
    @JsonBackReference("account-sentTransactions")
    private Ticket ticket;



    @ManyToOne
    @JoinColumn(name = "agentId", nullable = true)
    @JsonBackReference("account-sentTransactions")
    private Agent assignedTicket;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = true)
    @JsonBackReference("account-sentTransactions")
    private User ticketCreator;






    public Notification(String title, String description, Ticket ticket, Agent assignedTicket) {
        this.title = title;
        this.description = description;
        this.ticket = ticket;
        this.assignedTicket = assignedTicket;
    }
}
