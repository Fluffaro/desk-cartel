package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.NotificationRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for scheduled tasks related to ticket assignments.
 * Periodically checks for unassigned tickets and tries to assign them to available agents.
 */
@Service
@EnableScheduling
public class TicketAssignmentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TicketAssignmentScheduler.class);
    
    private final TicketRepository ticketRepository;
    private final AgentService agentService;
    private final NotificationRepository notificationRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    public TicketAssignmentScheduler(TicketRepository ticketRepository, AgentService agentService, NotificationRepository notificationRepository, AgentRepository agentRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.agentService = agentService;
        this.notificationRepository = notificationRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Scheduled task that runs every minute to check for unassigned tickets.
     * Tries to assign tickets with NO_AGENT_AVAILABLE status to available agents.
     */
    @Scheduled(fixedRate = 60000) // Run every minute (60,000 ms)
    @Transactional
    public void assignPendingTickets() {
        logger.info("Running scheduled task to assign pending tickets...");
        
        // Find all tickets with NO_AGENT_AVAILABLE status
        List<Ticket> unassignedTickets = ticketRepository.findByStatus(Status.NO_AGENT_AVAILABLE);
        
        if (unassignedTickets.isEmpty()) {
            logger.info("No pending tickets to assign.");
            return;
        }
        
        logger.info("Found {} unassigned tickets to process.", unassignedTickets.size());
        
        // Try to assign each ticket
        for (Ticket ticket : unassignedTickets) {
            logger.info("Attempting to assign ticket {} with priority {}...", 
                    ticket.getTicketId(), ticket.getPriority());
            
            Ticket updatedTicket = agentService.assignTicketToAgent(ticket.getTicketId());

            if (updatedTicket != null && updatedTicket.getAssignedTicket() != null) {
                // The notification to the agent is already sent by the agentService.assignTicketToAgent method
                
                // Notify the user that their ticket has been assigned to an agent
                // Create a user notification that their ticket has been assigned
                Notification userNotification = new Notification();
                userNotification.setTitle("Ticket Assigned: " + updatedTicket.getTitle());
                userNotification.setDescription("Your ticket #" + updatedTicket.getTicketId() + 
                        " has been assigned to an agent and is now being processed.");
                userNotification.setTicket(updatedTicket);
                userNotification.setAssignedTicket(updatedTicket.getAssignedTicket());
                userNotification.setTicketCreator(updatedTicket.getTicketOwner());
                
                // Increment user notification count
                if (updatedTicket.getTicketOwner() != null) {
                    Long userId = updatedTicket.getTicketOwner().getId();
                    Optional<User> userOpt = userRepository.findById(userId);
                    
                    if (userOpt.isPresent()) {
                        User userNotif = userOpt.get();
                        int userNotifCount = userNotif.getNotifCount();
                        userNotif.setNotifCount(userNotifCount + 1);
                        userRepository.save(userNotif);
                    }
                }
                
                notificationRepository.save(userNotification);
                
                logger.info("Successfully assigned ticket {} to agent {}.", 
                        updatedTicket.getTicketId(), updatedTicket.getAssignedTicket().getId());
            } else {
                logger.info("Still no suitable agent available for ticket {}.", ticket.getTicketId());
            }
        }
    }
} 