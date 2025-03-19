package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.TicketDTO;
import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.services.AgentService;
import com.ticket.desk_cartel.services.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    private final TicketService ticketService;
    private final AgentService agentService;

    @Autowired
    public TicketController(TicketService ticketService, AgentService agentService) {
        this.ticketService = ticketService;
        this.agentService = agentService;
    }

    /**
     * Get all tickets in the system. Admin access only.
     * 
     * @return List of all tickets
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can access this endpoint
    public ResponseEntity<List<Ticket>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get tickets created by a specific user.
     * 
     * @param userId The user ID
     * @return List of tickets created by the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUserId(@PathVariable Long userId) {
        List<Ticket> userTickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(userTickets);
    }

    /**
     * Get tickets assigned to a specific agent.
     * 
     * @param assignedAgent The agent ID
     * @return List of tickets assigned to the agent
     */
    @GetMapping("/agent/{assignedAgent}")
    public ResponseEntity<List<Ticket>> getTicketsByAgent(@PathVariable Long assignedAgent) {
        List<Ticket> agentTickets = ticketService.getTicketsByAgent(assignedAgent);
        return ResponseEntity.ok(agentTickets);
    }

    /**
     * Get a specific ticket by ID.
     * 
     * @param ticketId The ticket ID
     * @return The ticket or 404 if not found
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        if(ticket == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticket);
    }

    /**
     * Filter tickets based on various criteria.
     * 
     * @param category Category filter (optional)
     * @param priority Priority filter (optional)
     * @param status Status filter (optional)
     * @return List of matching tickets
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Ticket>> filterTicket(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Status status
            ) {
        List<Ticket> filteredTickets = ticketService.filterTickets(category, priority, status);
        return ResponseEntity.ok(filteredTickets);
    }

    /**
     * Create a new ticket.
     * The priority will be automatically determined by AI classification.
     * 
     * @param userId The user ID creating the ticket
     * @param ticketDTO The ticket data
     * @return The created ticket
     * @throws Exception if creation fails
     */
    @PostMapping()
    public ResponseEntity<Ticket> createTicket(@RequestParam Long userId, @RequestBody TicketDTO ticketDTO) throws Exception {
        // Validate that category is provided
        if (ticketDTO.getCategory() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        // Set status to ASSIGNED initially (will be auto-assigned to agent if possible)
        return ResponseEntity.ok(ticketService.createTicket(
                userId,
                ticketDTO.getTitle(),
                ticketDTO.getDescription(),
                Priority.NOT_ASSIGNED, // Let the AI determine the priority
                Status.ASSIGNED,       // Start with ASSIGNED status
                ticketDTO.getCategory()));
    }

    /**
     * Update the ticket's priority, category, or status.
     *
     * @param ticketId The ticket's ID to be updated
     * @param priority The new priority (optional)
     * @param categoryId The new category ID (optional)
     * @param status The new status (optional)
     * @param token JWT authorization token
     * @return Updated ticket details
     */
    @PutMapping("/{ticketId}")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Status status,
            @RequestHeader("Authorization") String token) {
        
        token = token.replace("Bearer ", "");
        Ticket updatedTicket = ticketService.updateTicket(ticketId, priority, categoryId, status, token);
        
        if (updatedTicket == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        return ResponseEntity.ok(updatedTicket);
    }

    /**
     * Assign a ticket to a specific agent. Admin only.
     * 
     * @param ticketId The ticket ID
     * @param agentId The agent ID
     * @param token JWT authorization token
     * @return The updated ticket
     */
    @PostMapping("/{ticketId}/assign/{agentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignTicketToAgent(
            @PathVariable Long ticketId,
            @PathVariable Long agentId,
            @RequestHeader("Authorization") String token) {
        
        token = token.replace("Bearer ", "");
        Ticket updatedTicket = ticketService.assignTicketToSpecificAgent(ticketId, agentId, token);
        
        if (updatedTicket == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Could not assign ticket to agent.");
            return ResponseEntity.badRequest().body(error);
        }
        
        return ResponseEntity.ok(updatedTicket);
    }

    /**
     * Auto-assign an unassigned ticket to the best available agent. Admin only.
     * 
     * @param ticketId The ticket ID
     * @return The updated ticket
     */
    @PostMapping("/{ticketId}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> autoAssignTicket(@PathVariable Long ticketId) {
        Ticket updatedTicket = ticketService.getTicketById(ticketId);
        
        if (updatedTicket == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Skip if ticket already has an agent
        if (updatedTicket.getAssignedTicket() != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ticket is already assigned to an agent.");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Auto-assign to an available agent
        Ticket assignedTicket = agentService.assignTicketToAgent(ticketId);
        
        if (assignedTicket == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Could not find a suitable agent for this ticket.");
            return ResponseEntity.badRequest().body(error);
        }
        
        return ResponseEntity.ok(assignedTicket);
    }

    /**
     * Mark a ticket as complete by the client who created it
     * 
     * @param ticketId The ticket ID to complete
     * @param userId The user ID of the client who created the ticket
     * @param token The JWT authorization token
     * @return The updated ticket or error response
     */
    @PostMapping("/{ticketId}/complete")
    public ResponseEntity<?> completeTicketByClient(
            @PathVariable Long ticketId,
            @RequestParam Long userId,
            @RequestHeader("Authorization") String token) {
        
        token = token.replace("Bearer ", "");
        
        Ticket updatedTicket = ticketService.completeTicketByClient(ticketId, userId, token);
        
        if (updatedTicket == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Could not complete ticket. Ensure you are the ticket creator and the ticket is in ONGOING status.");
            return ResponseEntity.badRequest().body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("ticket", updatedTicket);
        response.put("message", "Ticket #" + ticketId + " has been marked as completed by " + 
                     updatedTicket.getTicketOwner().getUsername() + ". Thank you for your feedback!");
        
        return ResponseEntity.ok(response);
    }
}
