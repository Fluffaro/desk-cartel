package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.CategoryRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.security.JwtUtil;
import jakarta.security.auth.message.AuthException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final CategoryRepository categoryRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository, JwtUtil jwtUtil, CategoryRepository categoryRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.categoryRepository = categoryRepository;
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
     * @param categoryId The new category to set (optional).
     * @param status The new status to set (optional).
     * @return The updated ticket.
     */
    public Ticket updateTicket(Long ticketId, Priority priority, Long categoryId, Status status, String token) {
        // Retrieve the ticket by its ID
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);

        if(ticket == null){
            return null;
        }

        String role = jwtUtil.extractRole(token);
        if("ADMIN".equals(role)) {
            if(priority != null) {
                ticket.setPriority(priority);
            }
            if(categoryId != null) {

                Category foundCategory = categoryRepository.findById(categoryId).orElse(null);
                if (foundCategory != null) {
                    ticket.setCategory(foundCategory);
                } else {
                    return null; // If the category is not found, return null
                }
            }
        }else if("AGENT".equals(role)){
            if(status != null ) {
                ticket.setStatus(status);

                if(status == Status.COMPLETED) {
                    ticket.setCompletion_date(LocalDateTime.now());
                }
            }
        }else {
            return null;
        }

        return ticketRepository.save(ticket);

    }

    /**
     * Updates only the priority of a ticket. To be used by admins only.
     *
     * @param ticketId The ID of the ticket to update.
     * @param priority The new priority to set.
     * @return The updated ticket or null if ticket not found.
     */
    public Ticket updateTicketPriority(Long ticketId, Priority priority) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            return null;
        }
        
        Ticket ticket = ticketOpt.get();
        ticket.setPriority(priority);
        
        return ticketRepository.save(ticket);
    }

    /*
    public Ticket updateTicketCompletionStatus(){
        Optional<Ticket> ticketOpt = ticketRepository.findById()
    }

     */

    public List<Ticket> getAllOnGoingTickets(){

        List<Ticket> ticketList = ticketRepository.findAll();

        List<Ticket> onGoingTicket = new ArrayList<>();


        for(int i=0; i< ticketList.size(); i++){
            if(LocalDateTime.now().isBefore(ticketList.get(i).getCompletion_date())){
                onGoingTicket.add(ticketList.get(i));
            }

        }
        return onGoingTicket;
    }

    /*
    public List<Ticket> getHoursDeadlineRemaining(){
        List<Ticket> onGoingTickets = getAllOnGoingTickets();
        List<Ticket> ticketHoursRemaining = new ArrayList<>();

        for(int i=0; i < onGoingTickets.size(); i++){
            LocalDateTime completionDate = onGoingTickets.get(i).getCompletion_date();


            long hoursBetween = Duration.between(LocalDateTime.now(), completionDate).toHours();


            ticketHoursRemaining.add((int) hoursBetween);

        }

        Collections.sort(hoursRemaining);



        return hoursRemaining;


    }

     */

    public List<Ticket> getHoursDeadlineRemaining() {
        List<Ticket> onGoingTickets = getAllOnGoingTickets();
        List<Ticket> ticketHoursRemaining = new ArrayList<>();


        for (Ticket ticket : onGoingTickets) {
            LocalDateTime completionDate = ticket.getCompletion_date();


            long hoursRemaining = Duration.between(LocalDateTime.now(), completionDate).toHours();


            ticket.setHoursRemaining((int) hoursRemaining);


            ticketHoursRemaining.add(ticket);
        }


        ticketHoursRemaining.sort(Comparator.comparingInt(Ticket::getHoursRemaining));

        return ticketHoursRemaining;
    }

    //comment



}
