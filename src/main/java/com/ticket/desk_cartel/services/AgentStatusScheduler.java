package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for scheduled tasks related to agent status checking.
 * Periodically checks for tickets assigned to inactive agents and reassigns them.
 */
@Service
@EnableScheduling
public class AgentStatusScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentStatusScheduler.class);
    
    private final TicketRepository ticketRepository;
    private final AgentRepository agentRepository;
    private final AgentService agentService;
    
    @Autowired
    public AgentStatusScheduler(
            TicketRepository ticketRepository,
            AgentRepository agentRepository,
            AgentService agentService) {
        this.ticketRepository = ticketRepository;
        this.agentRepository = agentRepository;
        this.agentService = agentService;
    }
    
    /**
     * Scheduled task that runs every 30 seconds to check for tickets assigned to inactive agents.
     * Reassigns any tickets found assigned to inactive agents.
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    public void checkInactiveAgentTickets() {
        logger.info("Running scheduled task to check for tickets assigned to inactive agents...");
        
        // Get all inactive agents
        List<Agent> inactiveAgents = agentRepository.findByIsActiveFalse();
        
        if (inactiveAgents.isEmpty()) {
            logger.debug("No inactive agents found.");
            return;
        }
        
        logger.info("Found {} inactive agents, checking for assigned tickets.", inactiveAgents.size());
        
        for (Agent agent : inactiveAgents) {
            // Find all active tickets assigned to this inactive agent
            List<Ticket> assignedTickets = ticketRepository.findByAssignedTicket_Id(agent.getId())
                .stream()
                .filter(ticket -> ticket.getStatus() != Status.COMPLETED)
                .collect(Collectors.toList());
                
            if (!assignedTickets.isEmpty()) {
                logger.warn("Found {} tickets still assigned to inactive agent {}. Reassigning tickets...", 
                        assignedTickets.size(), agent.getId());
                        
                agentService.reassignAgentTickets(agent);
            }
        }
    }
    
    /**
     * Verify all tickets in the system to ensure they're not assigned to inactive agents.
     * This is a more comprehensive check that can be run less frequently.
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void validateAllTicketAssignments() {
        logger.info("Running comprehensive ticket assignment validation...");
        
        List<Ticket> allActiveTickets = ticketRepository.findAll().stream()
            .filter(ticket -> ticket.getStatus() != Status.COMPLETED && ticket.getAssignedTicket() != null)
            .collect(Collectors.toList());
            
        int reassignedCount = 0;
        
        for (Ticket ticket : allActiveTickets) {
            Agent assignedAgent = ticket.getAssignedTicket();
            
            // Check if the assigned agent is inactive
            if (assignedAgent != null && !assignedAgent.isActive()) {
                logger.warn("Ticket {} is assigned to inactive agent {}. Reassigning...", 
                        ticket.getTicketId(), assignedAgent.getId());
                
                // Remove current agent assignment
                ticket.setAssignedTicket(null);
                ticket.setStatus(Status.NO_AGENT_AVAILABLE);
                ticketRepository.save(ticket);
                
                // Try to assign to a new agent
                agentService.assignTicketToAgent(ticket.getTicketId());
                reassignedCount++;
            }
        }
        
        if (reassignedCount > 0) {
            logger.info("Reassigned {} tickets from inactive agents.", reassignedCount);
        } else {
            logger.info("No tickets needed reassignment from inactive agents.");
        }
    }
} 