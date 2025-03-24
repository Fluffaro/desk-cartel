package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.TicketDTO;
import com.ticket.desk_cartel.dto.TicketSortDTO;
import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.services.AgentService;
import com.ticket.desk_cartel.services.TicketService;
import com.ticket.desk_cartel.services.PriorityService;
import com.ticket.desk_cartel.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${api.ticket.base-url}")
public class TicketController {

    private final TicketService ticketService;
    private final AgentService agentService;
    private final PriorityService priorityService;
    private final CategoryRepository categoryRepository;

    @Autowired
    public TicketController(TicketService ticketService, AgentService agentService, PriorityService priorityService, CategoryRepository categoryRepository) {
        this.ticketService = ticketService;
        this.agentService = agentService;
        this.priorityService = priorityService;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Get all tickets in the system. Admin access only.
     * 
     * @return List of all tickets
     */
    @GetMapping
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
    @GetMapping("${api.ticket.getTicketByUser}")
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
    @GetMapping("${api.ticket.getTicketByAgent}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
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
    @GetMapping("${api.ticket.ticketId}")
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
    @GetMapping("${api.ticket.filter}")
    public ResponseEntity<List<Ticket>> filterTickets(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long priorityId,
            @RequestParam(required = false) String statusName
            ) {
        // Convert IDs to objects
        Category category = (categoryId != null) ? 
            categoryRepository.findById(categoryId).orElse(null) : null;
        
        Priority priority = (priorityId != null) ?
            priorityService.getPriorityById(priorityId) : null;
        
        Status status = (statusName != null) ?
            Status.valueOf(statusName) : null;
        
        List<Ticket> filteredTickets = ticketService.filterTickets(category, priority, status);
        return ResponseEntity.ok(filteredTickets);
    }
    /**
     * Create a new ticket. Only accessible to clients, not agents.
     * Only title, description and categoryId are required.
     * Priority will be automatically determined by AI if not specified.
     * Status will default to ASSIGNED if not specified.
     * 
     * @param userId The user ID creating the ticket
     * @param ticketDTO The ticket data (title, description, categoryId required; priorityId and status optional)
     * @return The created ticket
     * @throws Exception if creation fails
     */
    @PostMapping("${api.ticket.create}")
    @PreAuthorize("hasRole('CLIENT') and !hasRole('AGENT')")  // Only clients can create tickets, not agents
    @Operation(
        summary = "Create a new ticket", 
        description = "Only title, description and categoryId are required. Priority will be automatically determined by AI if not specified. Status will default to ASSIGNED if not specified.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Ticket data",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TicketDTO.class),
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Minimal example",
                        summary = "Minimal required fields",
                        value = "{\"title\":\"Internet not working\",\"description\":\"I can't connect to the internet after restarting my computer\",\"categoryId\":1}"
                    ),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Full example",
                        summary = "Example with all fields",
                        value = "{\"title\":\"Internet not working\",\"description\":\"I can't connect to the internet after restarting my computer\",\"priorityId\":2,\"status\":\"ASSIGNED\",\"categoryId\":1}"
                    )
                }
            )
        )
    )
    @ApiResponse(responseCode = "200", description = "The created ticket", content = @Content(schema = @Schema(implementation = Ticket.class)))
    @ApiResponse(responseCode = "400", description = "Bad request")
    public ResponseEntity<Ticket> createTicket(
            @RequestParam Long userId, 
            @Valid @RequestBody TicketDTO ticketDTO) throws Exception {
        
        // Get the category from database
        Category category = null;
        try {
            category = categoryRepository.findById(ticketDTO.getCategoryId())
                .orElseThrow(() -> new Exception("Category not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
        
        // Get priority if specified, otherwise use null for AI classification
        Priority priority = null;
        if (ticketDTO.getPriorityId() != null) {
            try {
                priority = priorityService.getPriorityById(ticketDTO.getPriorityId());
            } catch (Exception e) {
                // If priority not found, we'll just use null and let AI decide
            }
        }
        
        // Set status to ASSIGNED initially (will be auto-assigned to agent if possible)
        Status status = ticketDTO.getStatus() != null ? ticketDTO.getStatus() : Status.ASSIGNED;
        
        return ResponseEntity.ok(ticketService.createTicket(
                userId,
                ticketDTO.getTitle(),
                ticketDTO.getDescription(),
                priority,          // Will be null if not specified, triggering AI classification
                status,            // Default or specified status
                category));
    }

    /**
     * Update the ticket's priority, category, or status.
     *
     * @param ticketId The ticket's ID to be updated
     * @param priorityId The new priority ID (optional)
     * @param categoryId The new category ID (optional)
     * @param status The new status (optional)
     * @param token JWT authorization token
     * @return Updated ticket details
     */
    @PutMapping("${api.ticket.update}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Long priorityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Status status,
            @RequestHeader("Authorization") String token) {
        
        Priority priority = null;
        if (priorityId != null) {
            try {
                priority = priorityService.getPriorityById(priorityId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
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
    @PostMapping("${api.ticket.assignAgent}")
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
    @PostMapping("${api.ticket.autoAssign}")
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
    @PostMapping("${api.ticket.complete}")
    @PreAuthorize("hasRole('CLIENT')")
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

    /**
     * Get all tickets assigned to an agent identified by user ID.
     *
     * @param userId The user ID
     * @return List of tickets assigned to the agent or 404 if user is not an agent
     */
    @GetMapping("${api.ticket.userid}")
    public ResponseEntity<List<Ticket>> getAgentTicketsByUser(@PathVariable Long userId) {
        Optional<Agent> agent = agentService.findAgentByUserId(userId);
        if (agent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticketService.getTicketsByAgent(agent.get().getId()));
    }

    /**
     * Get tickets sorted by the specified field and direction.
     * 
     * @param sortDTO Sorting parameters (field to sort by and direction)
     * @return List of sorted tickets
     */
    @GetMapping("/sort")
    @Operation(
        summary = "Get sorted tickets",
        description = "Returns tickets sorted by the specified field and direction"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved sorted tickets", 
                 content = @Content(schema = @Schema(implementation = Ticket.class)))
    public ResponseEntity<List<Ticket>> getSortedTickets(@Valid @RequestBody TicketSortDTO sortDTO) {
        List<Ticket> sortedTickets = ticketService.getSortedTickets(
            sortDTO.getSortBy(), 
            sortDTO.getDirection()
        );
        return ResponseEntity.ok(sortedTickets);
    }

    /**
     * Get tickets sorted by the specified field and direction using request parameters.
     * 
     * @param sortBy Field to sort by (default: "ticketId")
     * @param direction Sort direction (default: "ASC")
     * @return List of sorted tickets
     */
    @GetMapping("/sort-by")
    @Operation(
        summary = "Get sorted tickets using query parameters",
        description = "Returns tickets sorted by the specified field and direction using query parameters"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved sorted tickets", 
                 content = @Content(schema = @Schema(implementation = Ticket.class)))
    public ResponseEntity<List<Ticket>> getSortedTicketsByParams(
            @RequestParam(defaultValue = "ticketId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        List<Ticket> sortedTickets = ticketService.getSortedTickets(sortBy, direction);
        return ResponseEntity.ok(sortedTickets);
    }

    /**
     * Filter and sort tickets based on various criteria.
     * 
     * @param category Category filter (optional)
     * @param priority Priority filter (optional)
     * @param status Status filter (optional)
     * @param sortBy Field to sort by (default: "ticketId")
     * @param direction Sort direction (default: "ASC")
     * @return List of filtered and sorted tickets
     */
    @GetMapping("/filter-sort")
    @Operation(
        summary = "Filter and sort tickets",
        description = "Returns tickets filtered by the specified criteria and sorted by the specified field and direction"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered and sorted tickets", 
                 content = @Content(schema = @Schema(implementation = Ticket.class)))
    public ResponseEntity<List<Ticket>> filterAndSortTickets(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "ticketId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        List<Ticket> filteredAndSortedTickets = ticketService.filterAndSortTickets(
            category, 
            priority, 
            status, 
            sortBy, 
            direction
        );
        return ResponseEntity.ok(filteredAndSortedTickets);
    }
}
