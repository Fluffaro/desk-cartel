package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing support agents and ticket assignments.
 * Handles agent creation, ticket assignment, capacity management, and progression.
 */
@Service
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    
    @Autowired
    public AgentService(
            AgentRepository agentRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository) {
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }
    
    /**
     * Create a new agent from a user.
     * 
     * @param userId the ID of the user to convert to an agent
     * @return the newly created agent
     * @throws Exception if the user doesn't exist or is already an agent
     */
    @Transactional
    public Agent createAgent(Long userId) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new Exception("User not found with ID: " + userId);
        }
        
        User user = userOpt.get();
        
        // Check if user is already an agent
        Optional<Agent> existingAgent = agentRepository.findByUser(user);
        if (existingAgent.isPresent()) {
            throw new Exception("User is already an agent");
        }
        
        // Update user role to AGENT
        user.setRole("AGENT");
        userRepository.save(user);
        
        // Create new agent with JUNIOR level
        Agent agent = new Agent(user, AgentLevel.JUNIOR);
        return agentRepository.save(agent);
    }
    
    /**
     * Find the best available agent to handle a ticket based on priority and agent capacity.
     * 
     * @param priority the priority of the ticket
     * @return the best agent or empty if no suitable agent found
     */
    public Optional<Agent> findBestAgentForTicket(Priority priority) {
        if (priority == null) {
            logger.warn("Cannot find agent for null priority");
            return Optional.empty();
        }
        
        // Get weight of ticket
        int ticketWeight = priority.getWeight();
        
        logger.info("Finding best agent for ticket with priority {} (weight: {})", 
                priority.getName(), ticketWeight);
        
        // Find all active agents with enough capacity for this ticket
        List<Agent> availableAgents = agentRepository.findAgentsWithEnoughCapacityFor(ticketWeight);
        
        if (availableAgents.isEmpty()) {
            logger.warn("No available agents with enough capacity for ticket with weight: {}", ticketWeight);
            return Optional.empty();
        }
        
        logger.info("Found {} agents with sufficient capacity", availableAgents.size());
        
        // Find the best agent based on current workload percentage
        // This helps distribute work more evenly
        return availableAgents.stream()
                .min(Comparator.comparing(agent -> 
                        (double) agent.getCurrentWorkload() / agent.getTotalCapacity()));
    }
    
    /**
     * Assign a ticket to an agent.
     * 
     * @param ticketId the ID of the ticket to assign
     * @return the updated ticket with agent assigned or null if not possible
     */
    @Transactional
    public Ticket assignTicketToAgent(Long ticketId) {
        // Find the ticket
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            logger.warn("Cannot assign non-existent ticket: {}", ticketId);
            return null;
        }
        
        Ticket ticket = ticketOpt.get();
        
        // Skip if ticket already has an agent
        if (ticket.getAssignedTicket() != null) {
            logger.info("Ticket {} already assigned to agent {}", 
                    ticketId, ticket.getAssignedTicket().getId());
            return ticket;
        }
        
        // Find best agent for this ticket
        Optional<Agent> agentOpt = findBestAgentForTicket(ticket.getPriority());
        if (agentOpt.isEmpty()) {
            logger.warn("No suitable agent found for ticket: {}", ticketId);
            // Update status to indicate no agent is available
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            return ticketRepository.save(ticket);
        }
        
        Agent agent = agentOpt.get();
        
        // Double-check agent can handle this ticket's weight
        int ticketWeight = ticket.getPriority().getWeight();
        if (!agent.hasCapacityFor(ticketWeight)) {
            logger.warn("Agent {} cannot handle ticket {} with weight {}: current workload {} exceeds capacity {}",
                agent.getId(), ticketId, ticketWeight, agent.getCurrentWorkload(), agent.getTotalCapacity());
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            return ticketRepository.save(ticket);
        }
        
        // Assign the ticket to the agent
        ticket.setAssignedTicket(agent);
        ticket.setStatus(Status.ASSIGNED);
        
        // Update agent's workload
        agent.addWorkload(ticket.getPriority().getWeight());
        logger.info("Assigned ticket {} to agent {}. New workload: {}/{}",
            ticketId, agent.getId(), agent.getCurrentWorkload(), agent.getTotalCapacity());
        
        // Save changes
        agentRepository.save(agent);
        return ticketRepository.save(ticket);
    }
    
    /**
     * Start work on a ticket.
     * 
     * @param ticketId the ID of the ticket to start
     * @param agentId the ID of the agent starting the work
     * @return the updated ticket or null if not possible
     */
    @Transactional
    public Ticket startTicket(Long ticketId, Long agentId) {
        // Find the ticket
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            logger.warn("Cannot start non-existent ticket: {}", ticketId);
            return null;
        }
        
        Ticket ticket = ticketOpt.get();
        
        // Verify the ticket is assigned to this agent
        if (ticket.getAssignedTicket() == null || 
                !ticket.getAssignedTicket().getId().equals(agentId)) {
            logger.warn("Ticket {} is not assigned to agent {}", ticketId, agentId);
            return null;
        }
        
        // Verify ticket is in ASSIGNED status
        if (ticket.getStatus() != Status.ASSIGNED) {
            logger.warn("Cannot start ticket {} with status {}", ticketId, ticket.getStatus());
            return null;
        }
        
        // Update ticket status
        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(Status.ONGOING);
        ticket.setDate_started(now);
        
        // Calculate expected completion date based on priority time limit
        int hoursLimit = ticket.getPriority().getTimeLimit();
        LocalDateTime expectedCompletion = now.plusHours(hoursLimit);
        ticket.setExpected_completion_date(expectedCompletion);
        logger.info("Ticket {} started with expected completion in {} hours (by {})", 
                ticketId, hoursLimit, expectedCompletion);
        
        return ticketRepository.save(ticket);
    }
    
    /**
     * Complete a ticket and update agent progress.
     * Calculates performance points using the formula: (priority points * category points) / hours taken
     * 
     * @param ticketId the ID of the ticket to complete
     * @param agentId the ID of the agent completing the ticket
     * @return the completed ticket or null if not possible
     */
    @Transactional
    public Ticket completeTicket(Long ticketId, Long agentId) {
        // Find the ticket
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            logger.warn("Cannot complete non-existent ticket: {}", ticketId);
            return null;
        }
        
        Ticket ticket = ticketOpt.get();
        
        // Verify the ticket is assigned to this agent
        if (ticket.getAssignedTicket() == null || 
                !ticket.getAssignedTicket().getId().equals(agentId)) {
            logger.warn("Ticket {} is not assigned to agent {}", ticketId, agentId);
            return null;
        }
        
        // Verify ticket is in ONGOING status
        if (ticket.getStatus() != Status.ONGOING) {
            logger.warn("Cannot complete ticket {} with status {}", ticketId, ticket.getStatus());
            return null;
        }
        
        Agent agent = ticket.getAssignedTicket();
        
        // Set completion time
        LocalDateTime now = LocalDateTime.now();
        ticket.setCompletion_date(now);
        
        // Calculate performance points
        int performancePoints = calculatePerformancePoints(ticket, now);
        ticket.setPoints(performancePoints);
        
        logger.info("Agent {} earned {} performance points for completing ticket {}", 
                agentId, performancePoints, ticketId);
        
        // Update ticket
        ticket.setStatus(Status.COMPLETED);
        
        // Update agent's workload and completed tickets count with performance points
        agent.reduceWorkload(ticket.getPriority().getWeight());
        agent.addCompletedTicketWithPoints(performancePoints);
        
        // Save changes
        agentRepository.save(agent);
        return ticketRepository.save(ticket);
    }
    
    /**
     * Calculate performance points based on priority, category, and completion time.
     * Uses an efficiency-based formula that rewards early completion and slightly penalizes late completion.
     * 
     * @param ticket The completed ticket
     * @param completionTime The time when the ticket was completed
     * @return the calculated performance points
     */
    public int calculatePerformancePoints(Ticket ticket, LocalDateTime completionTime) {
        // Base points from priority and category
        int priorityPoints = ticket.getPriority().getWeight();
        int categoryPoints = ticket.getCategory().getPoints();
        int basePoints = priorityPoints * categoryPoints;
        
        // Get expected completion time from priority time limit
        LocalDateTime startTime = ticket.getDate_started();
        if (startTime == null) {
            logger.warn("Ticket {} has no start time recorded, using base points only", ticket.getTicketId());
            return basePoints; 
        }
        
        // Get the expected completion time
        LocalDateTime expectedCompletion = ticket.getExpected_completion_date();
        if (expectedCompletion == null) {
            // Fallback if not set
            expectedCompletion = startTime.plusHours(ticket.getPriority().getTimeLimit());
        }
        
        // Calculate time efficiency factor
        double expectedHours = java.time.Duration.between(startTime, expectedCompletion).toMinutes() / 60.0;
        double actualHours = java.time.Duration.between(startTime, completionTime).toMinutes() / 60.0;
        
        if (actualHours < 0.5) actualHours = 0.5; // Minimum time to prevent gaming
        
        // Calculate efficiency bonus (or penalty)
        double efficiencyFactor;
        if (actualHours <= expectedHours) {
            // Completed early or on time - bonus (max 50% bonus for being twice as fast)
            efficiencyFactor = 1.0 + Math.min(0.5, (expectedHours - actualHours) / expectedHours);
        } else {
            // Completed late - small penalty (max 20% reduction)
            efficiencyFactor = Math.max(0.8, 1.0 - ((actualHours - expectedHours) / expectedHours) * 0.2);
        }
        
        // Calculate final points
        int calculatedPoints = (int) Math.round(basePoints * efficiencyFactor);
        
        logger.info("Performance points: Base {} points * Efficiency factor {} = {} points", 
                basePoints, String.format("%.2f", efficiencyFactor), calculatedPoints);
        
        return calculatedPoints;
    }
    
    /**
     * Get all agents with their current stats.
     * 
     * @return list of all agents
     */
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }
    
    /**
     * Get an agent by ID.
     * 
     * @param agentId the agent ID
     * @return the agent or empty if not found
     */
    public Optional<Agent> getAgentById(Long agentId) {
        return agentRepository.findById(agentId);
    }
    
    /**
     * Find available agents for assignment.
     * 
     * @return list of agents with available capacity
     */
    public List<Agent> findAvailableAgents() {
        return agentRepository.findAvailableAgents();
    }
    
    /**
     * Set an agent's active status.
     * 
     * @param agentId the agent ID
     * @param active the new active status
     * @return the updated agent or empty if not found
     */
    @Transactional
    public Optional<Agent> setAgentActiveStatus(Long agentId, boolean active) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Agent agent = agentOpt.get();
        agent.setActive(active);
        return Optional.of(agentRepository.save(agent));
    }
    
    /**
     * Find an agent by user ID.
     * 
     * @param userId the user ID
     * @return the agent or empty if not found
     */
    public Optional<Agent> findAgentByUserId(Long userId) {
        return agentRepository.findByUserId(userId);
    }
    
    /**
     * Save an agent entity
     * 
     * @param agent The agent to save
     * @return The saved agent
     */
    public Agent saveAgent(Agent agent) {
        return agentRepository.save(agent);
    }

    @PostMapping("/notifCount/{id}")
    public ResponseEntity<?> getNotifCount(@PathVariable Long id){
        return agent
    }
} 