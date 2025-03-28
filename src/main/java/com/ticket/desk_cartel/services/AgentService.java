package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.NotificationRepository;
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
import java.util.stream.Collectors;

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
    private final NotificationRepository notificationRepository;
    private final VerificationTokenService verificationTokenService;
    
    @Autowired
    public NotificationService notificationService;
    
    @Autowired
    public AgentService(
            AgentRepository agentRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository, NotificationRepository notificationRepository,
            VerificationTokenService verificationTokenService) {
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.notificationRepository = notificationRepository;
        this.verificationTokenService = verificationTokenService;
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
            // Check if the assigned agent is still active
            if (!ticket.getAssignedTicket().isActive()) {
                logger.warn("Ticket {} is assigned to inactive agent {}. Will reassign.", 
                        ticketId, ticket.getAssignedTicket().getId());
                // Remove the inactive agent assignment
                Agent inactiveAgent = ticket.getAssignedTicket();
                ticket.setAssignedTicket(null);
                ticket.setStatus(Status.NO_AGENT_AVAILABLE);
                ticketRepository.save(ticket);
                
                // Reset agent's workload for this ticket
                if (inactiveAgent != null) {
                    inactiveAgent.reduceWorkload(ticket.getPriority().getWeight());
                    agentRepository.save(inactiveAgent);
                }
            } else {
                // Agent is active, so ticket is already properly assigned
                logger.info("Ticket {} already assigned to active agent {}", 
                        ticketId, ticket.getAssignedTicket().getId());
                return ticket;
            }
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
        
        // Verify the agent is active
        if (!agent.isActive()) {
            logger.warn("Selected agent {} is inactive. Cannot assign ticket {}", agent.getId(), ticketId);
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            return ticketRepository.save(ticket);
        }
        
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
        Ticket updatedTicket = ticketRepository.save(ticket);


        String emailSubject = "Ticket Notification";
        String emailText = "A new ticket has been assigned to you. Please check your dashboard for more details.";
        // Assuming you are sending the email to the agent's associated user email
        String agentEmail = ticket.getAssignedTicket().getUser().getEmail();
        // Call the sendEmail method
        verificationTokenService.sendEmail(agentEmail, emailSubject, emailText);

        // Create notification for the agent about the new assignment
        notificationService.createTicketAssignedNotification(updatedTicket);
        
        return updatedTicket;
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
        
        // Save the ticket
        Ticket updatedTicket = ticketRepository.save(ticket);

        String emailSubject = "Ticket Notification";
        String emailText = "Ticket has been assigned. Please check your dashboard for more details.";
        // Assuming you are sending the email to the agent's associated user email
        String agentEmail = ticket.getAssignedTicket().getUser().getEmail();
        String clientEmail = ticket.getTicketOwner().getEmail();
        // Call the sendEmail method
        verificationTokenService.sendEmail(agentEmail, emailSubject, emailText);
        verificationTokenService.sendEmail(clientEmail, emailSubject, emailText);

        // Create notification for the user that work has started
        notificationService.createTicketStartedNotification(updatedTicket);
        
        return updatedTicket;
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
        
        // Update agent's workload and completed tickets count with performance points
        agent.reduceWorkload(ticket.getPriority().getWeight());
        agent.addCompletedTicketWithPoints(performancePoints);
        
        // Update ticket status
        ticket.setStatus(Status.COMPLETED);
        
        // Save changes
        agentRepository.save(agent);
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Create notification for user about ticket completion by agent
        Notification userNotification = new Notification();
        userNotification.setTitle("Ticket Completed: " + updatedTicket.getTitle());
        userNotification.setDescription("Your ticket #" + updatedTicket.getTicketId() + 
                " has been completed by the agent. Thank you for using our support system.");
        userNotification.setTicket(updatedTicket);
        userNotification.setAssignedTicket(updatedTicket.getAssignedTicket());
        userNotification.setTicketCreator(updatedTicket.getTicketOwner());
        
        // Increment user notification count if possible
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
        
        return updatedTicket;
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
     * When deactivating an agent, their current tickets will be reassigned.
     * 
     * @param agentId the agent ID
     * @param active the new active status
     * @return the updated agent or empty if not found
     */
    @Transactional
    public Optional<Agent> setAgentActiveStatus(Long agentId, boolean active) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isEmpty()) {
            logger.warn("Cannot set active status for non-existent agent: {}", agentId);
            return Optional.empty();
        }
        
        Agent agent = agentOpt.get();
        
        // Only process if status is actually changing
        if (agent.isActive() == active) {
            logger.info("Agent {} status unchanged, already {}", 
                agentId, active ? "active" : "inactive");
                
            // Even if the agent is already inactive, check for any tickets that might still be assigned
            if (!active) {
                List<Ticket> assignedTickets = ticketRepository.findByAssignedTicket_Id(agentId);
                if (!assignedTickets.isEmpty()) {
                    logger.warn("Agent {} is already inactive but still has {} tickets assigned. Forcing reassignment.",
                        agentId, assignedTickets.size());
                    forceReassignTickets(agent, assignedTickets);
                }
            }
            
            return Optional.of(agent);
        }
        
        logger.info("Changing agent {} status from {} to {}", 
            agentId, agent.isActive() ? "active" : "inactive", active ? "active" : "inactive");
        
        // If we're deactivating an agent, reassign their tickets first
        if (!active) {
            logger.info("Deactivating agent {} and reassigning tickets", agentId);
            
            // Count how many tickets will be affected
            List<Ticket> activeTickets = ticketRepository.findByAssignedTicket_Id(agentId)
                .stream()
                .filter(ticket -> ticket.getStatus() != Status.COMPLETED)
                .collect(Collectors.toList());
                
            int ticketCount = activeTickets.size();
            if (ticketCount > 0) {
                logger.info("Agent {} has {} active tickets that will be reassigned", 
                    agentId, ticketCount);
                forceReassignTickets(agent, activeTickets);
            } else {
                logger.info("Agent {} has no active tickets to reassign", agentId);
            }
        }
        
        // Update agent status
        agent.setActive(active);
        Agent savedAgent = agentRepository.save(agent);
        logger.info("Agent {} active status changed to {}", agentId, active);
        
        // Verify tickets were actually reassigned if deactivating
        if (!active) {
            List<Ticket> remainingTickets = ticketRepository.findByAssignedTicket_Id(agentId)
                .stream()
                .filter(ticket -> ticket.getStatus() != Status.COMPLETED)
                .collect(Collectors.toList());
                
            if (!remainingTickets.isEmpty()) {
                logger.warn("Agent {} still has {} active tickets after deactivation. Forcing reassignment.", 
                    agentId, remainingTickets.size());
                forceReassignTickets(agent, remainingTickets);
            }
        }
        
        return Optional.of(savedAgent);
    }
    
    /**
     * Force reassignment of tickets from an agent without attempting to assign them to new agents.
     * This is used when we need to guarantee tickets are unassigned from an agent.
     * 
     * @param agent The agent to unassign tickets from
     * @param tickets The tickets to unassign
     */
    @Transactional
    private void forceReassignTickets(Agent agent, List<Ticket> tickets) {
        if (agent == null || tickets == null || tickets.isEmpty()) {
            return;
        }
        
        logger.info("Forcing reassignment of {} tickets from agent {}", tickets.size(), agent.getId());
        
        // Calculate total workload to remove
        int totalWorkload = tickets.stream()
            .mapToInt(ticket -> ticket.getPriority().getWeight())
            .sum();
            
        // Update agent's workload
        agent.setCurrentWorkload(Math.max(0, agent.getCurrentWorkload() - totalWorkload));
        agentRepository.save(agent);
        
        // Unassign all tickets
        for (Ticket ticket : tickets) {
            ticket.setAssignedTicket(null);
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            ticketRepository.save(ticket);
            
            // Create notification for user
            notificationService.createNoAgentAvailableNotification(ticket);
            
            logger.info("Unassigned ticket {} from agent {}", ticket.getTicketId(), agent.getId());
        }
    }
    
    /**
     * Find an agent by user ID.
     * 
     * @param userId the user ID
     * @return the agent or empty if not found
     */
    public Optional<Agent> findAgentByUserId(Long userId) {
        Optional<Agent> agent = agentRepository.findByUserId(userId);
        if (agent.isEmpty()) {
            System.out.println("No agent found for userId: " + userId);
        }
        return agent;
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
    
    /**
     * Reassign all tickets from an inactive agent.
     * 
     * @param agent The agent whose tickets need to be reassigned
     */
    @Transactional
    public void reassignAgentTickets(Agent agent) {
        if (agent == null) {
            logger.warn("Cannot reassign tickets for null agent");
            return;
        }

        logger.info("Starting ticket reassignment for agent {}", agent.getId());

        // Find all tickets assigned to this agent that are not completed
        List<Ticket> agentTickets = ticketRepository.findByAssignedTicket_Id(agent.getId())
            .stream()
            .filter(ticket -> ticket.getStatus() != Status.COMPLETED)
            .collect(Collectors.toList());

        if (agentTickets.isEmpty()) {
            logger.info("No active tickets to reassign for agent {}", agent.getId());
            return;
        }

        logger.info("Found {} tickets to reassign from agent {}", agentTickets.size(), agent.getId());

        // Calculate total workload to be removed
        int totalWorkloadToRemove = agentTickets.stream()
            .mapToInt(ticket -> ticket.getPriority().getWeight())
            .sum();
            
        // Reset agent's workload
        agent.setCurrentWorkload(Math.max(0, agent.getCurrentWorkload() - totalWorkloadToRemove));
        agentRepository.save(agent);
        
        logger.info("Reset workload for agent {}. Removed {} points of workload.", 
            agent.getId(), totalWorkloadToRemove);

        // Try to reassign each ticket
        int reassignedCount = 0;
        for (Ticket ticket : agentTickets) {
            logger.info("Processing ticket {} with priority {}", 
                ticket.getTicketId(), ticket.getPriority().getName());
                
            // Remove current agent assignment
            ticket.setAssignedTicket(null);
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            ticketRepository.save(ticket);

            // Try to assign to a new agent
            Ticket updatedTicket = assignTicketToAgent(ticket.getTicketId());

            if (updatedTicket != null && updatedTicket.getAssignedTicket() != null) {
                logger.info("Successfully reassigned ticket {} from agent {} to agent {}", 
                    ticket.getTicketId(), agent.getId(), updatedTicket.getAssignedTicket().getId());
                reassignedCount++;
            } else {
                logger.warn("Could not find new agent for ticket {}. Marked as NO_AGENT_AVAILABLE", 
                    ticket.getTicketId());
                
                // Create notification for user about no agent being available
                notificationService.createNoAgentAvailableNotification(ticket);
            }
        }
        
        logger.info("Completed reassignment for agent {}. Successfully reassigned {} out of {} tickets.",
            agent.getId(), reassignedCount, agentTickets.size());
    }
} 