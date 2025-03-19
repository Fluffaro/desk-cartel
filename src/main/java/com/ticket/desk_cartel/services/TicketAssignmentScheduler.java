package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    
    @Autowired
    public TicketAssignmentScheduler(TicketRepository ticketRepository, AgentService agentService) {
        this.ticketRepository = ticketRepository;
        this.agentService = agentService;
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
                logger.info("Successfully assigned ticket {} to agent {}.", 
                        updatedTicket.getTicketId(), updatedTicket.getAssignedTicket().getId());
            } else {
                logger.info("Still no suitable agent available for ticket {}.", ticket.getTicketId());
            }
        }
    }
} 