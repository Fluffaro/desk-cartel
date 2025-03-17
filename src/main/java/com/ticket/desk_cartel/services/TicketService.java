package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.entities.User;
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

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public Ticket createTicket(Long userId, String title, String description, Priority priority, Status status, String category) throws Exception {
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
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setDate_started(LocalDateTime.now());
        ticket.setCompletion_date(null);

        return ticketRepository.save(ticket);
    }





}
