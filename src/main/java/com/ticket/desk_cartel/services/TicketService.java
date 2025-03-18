package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository, NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Ticket createTicket(Long userId, String title, String description, Priority priority, Status status, Category category) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new Exception("User not found");
        }

        User user = userOpt.get();

        Ticket ticket = new Ticket();
        ticket.setAssignedTicket(null);
        ticket.setTicketOwner(user);
        ticket.setPoints(0);
        ticket.setTitle(title);
        ticket.setPriority(priority != null ? priority : Priority.NOT_ASSIGNED);
        ticket.setStatus(status);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setDate_started(LocalDateTime.now());
        ticket.setCompletion_date(null);

        Ticket savedTicket = ticketRepository.save(ticket);
        
        notificationService.notifyTicketCreated(savedTicket);

        return savedTicket;
    }

    /**
     * Assign a ticket to an agent and send a notification
     */
    public Ticket assignTicket(Long ticketId, Agent agent) throws Exception {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            throw new Exception("Ticket not found");
        }

        Ticket ticket = ticketOpt.get();
        ticket.setAssignedTicket(agent);
        
        if (ticket.getStatus() == Status.OPEN) {
            ticket.setStatus(Status.IN_PROGRESS);
        }
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        notificationService.notifyTicketAssigned(savedTicket);
        
        return savedTicket;
    }

    /**
     * Update ticket priority and potentially trigger assignment
     */
    public Ticket updateTicketPriority(Long ticketId, Priority priority) throws Exception {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            throw new Exception("Ticket not found");
        }

        Ticket ticket = ticketOpt.get();
        
        ticket.setPriority(priority);
        Ticket savedTicket = ticketRepository.save(ticket);
        
        return savedTicket;
    }

    /**
     * Check ticket deadlines and send notifications if needed
     * This would typically be called by a scheduled task
     */
    public void checkTicketDeadlines() {
        ticketRepository.findAll().stream()
            .filter(ticket -> ticket.getStatus() != Status.RESOLVED && ticket.getStatus() != Status.CLOSED)
            .forEach(ticket -> notificationService.notifyDeadlineWarning(ticket));
    }
}
