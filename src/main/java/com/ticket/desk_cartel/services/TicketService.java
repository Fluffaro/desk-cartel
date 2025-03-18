package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
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

        return ticketRepository.save(ticket);
    }


    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsByUserId(Long userId) {
        return ticketRepository.findByTicketOwner_Id(userId);
    }


    public List<Ticket> getTicketsByAgent(Long assignedAgent) {
        return  ticketRepository.findByAssignedTicket_Id(assignedAgent);
    }

    public Ticket getTicketById(Long ticketId) {
        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        return ticket.orElse(null);
    }

    //Filter Needs adjustment
    public List<Ticket> filterTickets(Category category, Priority priority, Status status) {
        if(category != null && priority != null && status != null){
            return ticketRepository.findByCategoryAndPriorityAndStatus(category, priority,status);
        }else if(category != null && priority != null) {
            return ticketRepository.findByCategoryAndPriority(category, priority);
        }else if(category != null && status != null) {
            return ticketRepository.findByCategoryAndStatus(category, status);
        }else if(status != null && priority != null) {
            return ticketRepository.findByPriorityAndStatus(priority, status);
        }else if(status != null ) {
            return ticketRepository.findByStatus(status);
        }else if(priority != null ) {
            return ticketRepository.findByPriority(priority);
        }else if(category != null ) {
            return ticketRepository.findByCategory(category);
        }else {
            return ticketRepository.findAll();
        }
    }

    /**
     * Updates the ticket's priority, category, or status.
     * Admins can update priority and category, while agents can only update status.
     *
     * @param ticketId The ID of the ticket to update.
     * @param priority The new priority to set (optional).
     * @param category The new category to set (optional).
     * @param status The new status to set (optional).
     * @return The updated ticket.
     */
    public Ticket updateTicket(Long ticketId, Priority priority, Category category, Status status) {
        // Retrieve the ticket by its ID
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);

        if (ticket == null) {
            return null;  // Ticket not found, return null
        }

        // Get the current authenticated user
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getDetails();

        // Check if the current user is an admin or agent
        String role = currentUser.getRole(); // Assuming the role is stored in the User object

        if ("ADMIN".equals(role)) {
            // Admin can update priority and category
            if (priority != null) {
                ticket.setPriority(priority);
            }
            if (category != null) {
                ticket.setCategory(category);
            }
        } else if ("AGENT".equals(role)) {
            // Agent can only update the ticket's status
            if (status != null) {
                ticket.setStatus(status);
            }
        } else {
            // Unauthorized role, return null or throw exception
            return null;
        }

        // If the ticket has a completion date and the current time is after the completion date,
        // automatically set the ticket status to "Complete".
        if (ticket.getCompletion_date() != null && LocalDateTime.now().isAfter(ticket.getCompletion_date())) {
            ticket.setStatus(Status.COMPLETED);
        }

        // Save the updated ticket to the repository
        return ticketRepository.save(ticket);
    }
}
