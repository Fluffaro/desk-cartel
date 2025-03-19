package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.TicketDTO;
import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.services.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }


    //get all tickets
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can access this endpoint
    public ResponseEntity<List<Ticket>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }



    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUserId(@RequestParam Long userId) {
        List<Ticket> userTickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(userTickets);
    }

    @GetMapping("/agent/{assignedAgent}")
    public ResponseEntity<List<Ticket>> getTicketsByAgent(@RequestParam Long assignedAgent) {
        List<Ticket> agentTickets = ticketService.getTicketsByAgent(assignedAgent);
        return ResponseEntity.ok(agentTickets);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<Ticket> getTicketById(@RequestParam Long ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        if(ticket == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ticket);
    }
    //Need to create filter  backend
//
//    @GetMapping("/admin/no-priority")
//    public ResponseEntity<List<Ticket>> getNoPriorityTickets() {
//        List<Ticket> noPriorityTickets = ticketService.getNoPriorityTickets();
//        return ResponseEntity.ok(noPriorityTickets);
//    }

    @GetMapping("/filter")
    public ResponseEntity<List<Ticket>> filterTicket(
            @RequestParam(required = false)Category category,
            @RequestParam(required = false)Priority priority,
            @RequestParam(required = false)Status status
            ) {
        List<Ticket> filteredTickets = ticketService.filterTickets(category, priority, status);
        return  ResponseEntity.ok(filteredTickets);
    }

    @PostMapping()
    public ResponseEntity<Ticket> createTicket(@RequestParam Long userId, @RequestBody TicketDTO ticketDTO) throws Exception {
        // Validate that category is provided
        if (ticketDTO.getCategory() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        // Force priority to be NOT_ASSIGNED as only admins can set it later
        return ResponseEntity.ok(ticketService.createTicket(userId,
                ticketDTO.getTitle(),
                ticketDTO.getDescription(),
                Priority.NOT_ASSIGNED, // Always set to NOT_ASSIGNED on creation
                ticketDTO.getStatus(),
                ticketDTO.getCategory()));
    }

    /**
     * Update the ticket's priority, category, or status.
     *
     * @param ticketId The ticket's ID to be updated.
     * @param priority The new priority (can be null if the admin does not wish to update).
     * @param categoryId The new category (can be null if the admin does not wish to update).
     * @param status The new status (can be null if the agent does not wish to update).
     * @return Updated ticket details.
     */

    @PutMapping("/{ticketId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")  // Only admins or agents can access this endpoint
    public ResponseEntity<String> updateTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String token
    ) {
        Ticket updateTicket = ticketService.updateTicket(ticketId, priority, categoryId, status, token);

        if(updateTicket == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized or Ticket Not Found");
        }

        return ResponseEntity.ok("Ticket updated successfully");

    }

    @PutMapping("/{ticketId}/priority")
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can update priority
    public ResponseEntity<String> updateTicketPriority(
            @PathVariable Long ticketId,
            @RequestParam Priority priority
    ) {
        Ticket updatedTicket = ticketService.updateTicketPriority(ticketId, priority);

        if(updatedTicket == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ticket Not Found");
        }

        return ResponseEntity.ok("Ticket priority updated successfully");
    }

}
