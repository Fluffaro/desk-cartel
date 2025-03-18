package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import jakarta.security.auth.message.AuthException;
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
}
