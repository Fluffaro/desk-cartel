package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.*;
import com.ticket.desk_cartel.security.JwtUtil;
import jakarta.security.auth.message.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final CategoryRepository categoryRepository;
    private final NotificationRepository notificationRepository;
    private final AgentRepository agentRepository;

    @Autowired
    private GeminiAIService geminiAIService;
    
    @Autowired
    private AgentService agentService;
    
    @Autowired
    private PriorityService priorityService;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository,
                         JwtUtil jwtUtil, CategoryRepository categoryRepository, NotificationRepository notificationRepository, AgentRepository agentRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.categoryRepository = categoryRepository;
        this.notificationRepository = notificationRepository;
        this.agentRepository = agentRepository;
    }

    /**
     * Creates a new ticket with the given details.
     * If priority is not specified, it will be determined by AI.
     * The ticket will be auto-assigned to an agent if possible.
     * 
     * @param userId ID of the user creating the ticket
     * @param title Ticket title
     * @param description Ticket description
     * @param priority Priority level (optional, will be determined by AI if null)
     * @param status Initial status (optional)
     * @param category Ticket category
     * @return The created and potentially assigned ticket
     * @throws Exception if user not found or other error occurs
     */
    @Transactional
    public Ticket createTicket(Long userId, String title, String description, 
                             Priority priority, Status status, Category category) throws Exception {
        
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
        
        // Get default NOT_ASSIGNED priority if null
        Priority notAssignedPriority = null;
        try {
            notAssignedPriority = priorityService.getPriorityByName("NOT_ASSIGNED");
        } catch (Exception e) {
            logger.warn("NOT_ASSIGNED priority not found, using first available priority");
            notAssignedPriority = priorityService.getAllPriorities().get(0);
        }
        
        ticket.setPriority(priority != null ? priority : notAssignedPriority);
        ticket.setStatus(Status.ASSIGNED); // Default to ASSIGNED initially
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setDate_started(null); // Will be set when agent starts working
        ticket.setCompletion_date(null);

        // If priority is NOT_ASSIGNED or null, use Gemini AI to suggest priority
        if (priority == null || 
            (notAssignedPriority != null && priority.getName().equals(notAssignedPriority.getName()))) {
            Priority suggestedPriority = geminiAIService.suggestPriority(
                title, description, category.getName()
            );
            ticket.setPriority(suggestedPriority);
            logger.info("Auto-classified ticket priority: {}", suggestedPriority.getName());
        }

        // Save the ticket first
        ticket = ticketRepository.save(ticket);
        
        // Try to auto-assign the ticket to an agent
        Ticket assignedTicket = agentService.assignTicketToAgent(ticket.getTicketId());
        
        if (assignedTicket != null && assignedTicket.getAssignedTicket() != null) {
            logger.info("Successfully auto-assigned ticket {} to agent {}", 
                    assignedTicket.getTicketId(), 
                    assignedTicket.getAssignedTicket().getId());
            return assignedTicket;
        } else {
            // No suitable agent found - update status to NO_AGENT_AVAILABLE
            ticket.setStatus(Status.NO_AGENT_AVAILABLE);
            ticket = ticketRepository.save(ticket);
            logger.warn("Could not auto-assign ticket {}. No suitable agent available.", 
                    ticket.getTicketId());

            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setDescription(description);
            notification.setTicket(ticket);
            notification.setAssignedTicket(ticket.getAssignedTicket());
            Optional<Long> agentId = agentService.getAgentById(ticket.getAssignedTicket().getId());

            Long id = agentId.get();
            agentRepository.findById(agentId);

            notificationRepository.save(notification);
            return ticket;
        }
    }

    /**
     * Gets all tickets in the system.
     * 
     * @return A list of all tickets
     */
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    /**
     * Gets all tickets created by a specific user.
     * 
     * @param userId ID of the user
     * @return A list of tickets created by the user
     */
    public List<Ticket> getTicketsByUserId(Long userId) {
        return ticketRepository.findByTicketOwner_Id(userId);
    }

    /**
     * Gets all tickets assigned to a specific agent.
     * 
     * @param assignedAgent ID of the agent
     * @return A list of tickets assigned to the agent
     */
    public List<Ticket> getTicketsByAgent(Long assignedAgent) {
        return ticketRepository.findByAssignedTicket_Id(assignedAgent);
    }

    /**
     * Gets a specific ticket by its ID.
     * 
     * @param ticketId ID of the ticket
     * @return The ticket or null if not found
     */
    public Ticket getTicketById(Long ticketId) {
        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        return ticket.orElse(null);
    }

    /**
     * Filters tickets based on category, priority and status.
     * 
     * @param category Category filter (optional)
     * @param priority Priority filter (optional)
     * @param status Status filter (optional)
     * @return A list of tickets matching the filters
     */
    public List<Ticket> filterTickets(Category category, Priority priority, Status status) {
        if(category != null && priority != null && status != null){
            return ticketRepository.findByCategoryAndPriorityAndStatus(category, priority, status);
        } else if(category != null && priority != null) {
            return ticketRepository.findByCategoryAndPriority(category, priority);
        } else if(category != null && status != null) {
            return ticketRepository.findByCategoryAndStatus(category, status);
        } else if(priority != null && status != null) {
            return ticketRepository.findByPriorityAndStatus(priority, status);
        } else if(category != null) {
            return ticketRepository.findByCategory(category);
        } else if(priority != null) {
            return ticketRepository.findByPriority(priority);
        } else if(status != null) {
            return ticketRepository.findByStatus(status);
        } else {
            return ticketRepository.findAll();
        }
    }

    /**
     * Updates ticket properties based on user role.
     * Admins can update priority and category, while agents can update status.
     *
     * @param ticketId ID of the ticket to update
     * @param priority New priority (optional)
     * @param categoryId New category ID (optional)
     * @param status New status (optional)
     * @param token JWT token for authorization
     * @return The updated ticket or null if update failed
     */
    @Transactional
    public Ticket updateTicket(Long ticketId, Priority priority, Long categoryId, 
                              Status status, String token) {
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
        } else if("AGENT".equals(role)){
            if(status != null) {
                // Check if we're setting to ONGOING from ASSIGNED
                if (status == Status.ONGOING && ticket.getStatus() == Status.ASSIGNED) {
                    // Record start time when agent begins work
                    ticket.setDate_started(LocalDateTime.now());
                }
                
                // Check if we're setting to COMPLETED
                if(status == Status.COMPLETED) {
                    ticket.setCompletion_date(LocalDateTime.now());
                    
                    // Update agent's completed tickets and workload
                    if (ticket.getAssignedTicket() != null) {
                        Agent agent = ticket.getAssignedTicket();
                        agent.reduceWorkload(ticket.getPriority().getWeight());
                        agent.incrementCompletedTickets();
                    }
                }
                
                ticket.setStatus(status);
            }
        } else {
            return null;
        }

        return ticketRepository.save(ticket);
    }
    
    /**
     * Starts work on a ticket by an agent.
     * 
     * @param ticketId ID of the ticket
     * @param agentId ID of the agent starting work
     * @param token JWT token for authorization
     * @return The updated ticket or null if operation failed
     */
    @Transactional
    public Ticket startTicket(Long ticketId, Long agentId, String token) {
        // Verify agent permission
        String role = jwtUtil.extractRole(token);
        if (!"AGENT".equals(role)) {
            logger.warn("Non-agent attempted to start ticket {}", ticketId);
            return null;
        }
        
        // Use the agent service to start the ticket
        return agentService.startTicket(ticketId, agentId);
    }
    
    /**
     * Manually assigns a ticket to a specific agent.
     * Can be used by admins to override automatic assignment.
     * 
     * @param ticketId ID of the ticket
     * @param agentId ID of the agent
     * @param token JWT token for authorization
     * @return The updated ticket or null if operation failed
     */
    @Transactional
    public Ticket assignTicketToSpecificAgent(Long ticketId, Long agentId, String token) {
        // Only admins can manually assign tickets
        String role = jwtUtil.extractRole(token);
        if (!"ADMIN".equals(role)) {
            logger.warn("Non-admin attempted to assign ticket {} to agent {}", ticketId, agentId);
            return null;
        }
        
        // Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return null;
        }
        
        // If ticket is already assigned to an agent, remove it from their workload
        if (ticket.getAssignedTicket() != null) {
            Agent currentAgent = ticket.getAssignedTicket();
            currentAgent.reduceWorkload(ticket.getPriority().getWeight());
        }
        
        // Find the new agent
        Optional<Agent> agentOpt = agentService.getAgentById(agentId);
        if (agentOpt.isEmpty()) {
            return null;
        }
        
        Agent agent = agentOpt.get();
        
        // Assign ticket to new agent
        ticket.setAssignedTicket(agent);
        ticket.setStatus(Status.ASSIGNED);
        
        // Update agent workload
        agent.addWorkload(ticket.getPriority().getWeight());
        
        // Save changes
        return ticketRepository.save(ticket);
    }

    /**
     * Completes a ticket by the client who created it.
     * This marks the ticket as COMPLETED, sets the completion date,
     * and calculates performance points for the assigned agent.
     * 
     * @param ticketId ID of the ticket
     * @param userId ID of the client who created the ticket
     * @param token JWT token for authorization
     * @return The updated ticket or null if operation failed
     */
    @Transactional
    public Ticket completeTicketByClient(Long ticketId, Long userId, String token) {
        // Find the ticket
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            logger.warn("Cannot complete non-existent ticket: {}", ticketId);
            return null;
        }
        
        Ticket ticket = ticketOpt.get();
        
        // Verify the user is the ticket creator
        if (ticket.getTicketOwner() == null || 
                !ticket.getTicketOwner().getId().equals(userId)) {
            logger.warn("User {} is not the creator of ticket {}", userId, ticketId);
            return null;
        }
        
        // Verify ticket is in ONGOING status
        if (ticket.getStatus() != Status.ONGOING) {
            logger.warn("Cannot complete ticket {} with status {}", ticketId, ticket.getStatus());
            return null;
        }
        
        // Set completion time
        LocalDateTime now = LocalDateTime.now();
        ticket.setCompletion_date(now);
        
        // Calculate performance points if there is an assigned agent
        if (ticket.getAssignedTicket() != null) {
            Agent agent = ticket.getAssignedTicket();
            int performancePoints = agentService.calculatePerformancePoints(ticket, now);
            ticket.setPoints(performancePoints);
            
            logger.info("Agent {} earned {} performance points for completing ticket {}", 
                    agent.getId(), performancePoints, ticketId);
            
            // Update agent's workload and completed tickets count with performance points
            agent.reduceWorkload(ticket.getPriority().getWeight());
            agent.addCompletedTicketWithPoints(performancePoints);
            
            // Save agent changes
            agentService.saveAgent(agent);
        }
        
        // Update ticket status
        ticket.setStatus(Status.COMPLETED);
        
        // Save and return the updated ticket
        return ticketRepository.save(ticket);
    }
}
