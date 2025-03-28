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
import java.util.Map;
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
    private final NotificationService notificationService;
    
    @Autowired
    public AgentStatusScheduler(
            TicketRepository ticketRepository,
            AgentRepository agentRepository,
            AgentService agentService,
            NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.agentRepository = agentRepository;
        this.agentService = agentService;
        this.notificationService = notificationService;
    }
    
    /**
     * Scheduled task that runs every 30 seconds to check for tickets assigned to inactive agents.
     * Reassigns any tickets found assigned to inactive agents.
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    public void checkInactiveAgentTickets() {
        logger.info("Running scheduled task to check for tickets assigned to inactive agents...");
        
        // First approach: Check all agents marked as inactive
        List<Agent> inactiveAgents = agentRepository.findByIsActiveFalse();
        
        if (!inactiveAgents.isEmpty()) {
            logger.info("Found {} inactive agents, checking for assigned tickets.", inactiveAgents.size());
            
            for (Agent agent : inactiveAgents) {
                // Find all active tickets assigned to this inactive agent
                List<Ticket> assignedTickets = ticketRepository.findByAssignedTicket_Id(agent.getId())
                    .stream()
                    .filter(ticket -> ticket.getStatus() != Status.COMPLETED)
                    .collect(Collectors.toList());
                    
                if (!assignedTickets.isEmpty()) {
                    logger.warn("Found {} tickets still assigned to inactive agent {}. Forcing unassignment...", 
                            assignedTickets.size(), agent.getId());
                            
                    // Directly unassign tickets rather than using the normal reassignment logic
                    forceUnassignTickets(agent, assignedTickets);
                }
            }
        }
        
        // Second approach: Check all tickets to see if they're assigned to inactive agents
        // This is a more comprehensive check that catches edge cases
        List<Ticket> allActiveTickets = ticketRepository.findAll().stream()
            .filter(ticket -> 
                ticket.getStatus() != Status.COMPLETED && 
                ticket.getAssignedTicket() != null &&
                !ticket.getAssignedTicket().isActive())
            .collect(Collectors.toList());
            
        if (!allActiveTickets.isEmpty()) {
            logger.warn("Found {} tickets assigned to inactive agents through direct ticket check", 
                allActiveTickets.size());
                
            // Group tickets by agent
            Map<Agent, List<Ticket>> ticketsByAgent = allActiveTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getAssignedTicket));
                
            // Process each agent's tickets
            for (Map.Entry<Agent, List<Ticket>> entry : ticketsByAgent.entrySet()) {
                Agent agent = entry.getKey();
                List<Ticket> tickets = entry.getValue();
                
                logger.warn("Agent {} (inactive) has {} tickets still assigned", 
                    agent.getId(), tickets.size());
                    
                forceUnassignTickets(agent, tickets);
            }
        }
    }
    
    /**
     * Force unassignment of tickets from an inactive agent.
     * This method directly unassigns tickets without trying to reassign them to new agents.
     * 
     * @param agent The inactive agent
     * @param tickets The tickets to unassign
     */
    @Transactional
    private void forceUnassignTickets(Agent agent, List<Ticket> tickets) {
        if (agent == null || tickets == null || tickets.isEmpty()) {
            return;
        }
        
        logger.info("Force unassigning {} tickets from inactive agent {}", tickets.size(), agent.getId());
        
        // Calculate total workload to be removed
        int totalWorkloadToRemove = tickets.stream()
            .mapToInt(ticket -> ticket.getPriority().getWeight())
            .sum();
            
        // Reset agent's workload for these tickets
        agent.setCurrentWorkload(Math.max(0, agent.getCurrentWorkload() - totalWorkloadToRemove));
        agentRepository.save(agent);
        
        // Process each ticket
        for (Ticket ticket : tickets) {
            ticket.setAssignedTicket(null);
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            ticketRepository.save(ticket);
            
            // Create notification
            notificationService.createNoAgentAvailableNotification(ticket);
            
            logger.info("Unassigned ticket {} from inactive agent {}", ticket.getTicketId(), agent.getId());
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
                logger.warn("Ticket {} is assigned to inactive agent {}. Unassigning...", 
                        ticket.getTicketId(), assignedAgent.getId());
                
                // Reduce agent's workload
                assignedAgent.reduceWorkload(ticket.getPriority().getWeight());
                agentRepository.save(assignedAgent);
                
                // Remove agent assignment
                ticket.setAssignedTicket(null);
                ticket.setStatus(Status.NO_AGENT_AVAILABLE);
                ticketRepository.save(ticket);
                
                // Create notification
                notificationService.createNoAgentAvailableNotification(ticket);
                
                reassignedCount++;
            }
        }
        
        if (reassignedCount > 0) {
            logger.info("Unassigned {} tickets from inactive agents.", reassignedCount);
        }
    }
} 