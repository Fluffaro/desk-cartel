package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
}
