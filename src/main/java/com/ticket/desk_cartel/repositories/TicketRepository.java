package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ticket.desk_cartel.entities.Agent;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {


    List<Ticket> findByTicketOwner_Id(Long userId);
    List<Ticket> findByAssignedTicket_Id(Long assignedAgent);

    //Filters for ticket
    List<Ticket> findByCategoryAndPriorityAndStatus(Category category, Priority priority, Status status);
    List<Ticket> findByCategoryAndPriority(Category category, Priority priority);
    List<Ticket> findByCategoryAndStatus(Category category, Status status);
    List<Ticket> findByPriorityAndStatus(Priority priority, Status status);
    List<Ticket> findByCategory(Category category);
    List<Ticket> findByPriority(Priority priority);
    List<Ticket> findByStatus(Status status);

    // ✅ New method: Count ongoing tickets for an agent
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTicket = :agent AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    int countOngoingTickets(@Param("agent") Agent agent);

}
