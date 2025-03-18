package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.TicketDTO;
import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.services.TicketService;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.ok(ticketService.createTicket(userId,
                ticketDTO.getTitle(),
                ticketDTO.getDescription(),
                ticketDTO.getPriority(),
                ticketDTO.getStatus(),
                ticketDTO.getCategory() ));
    }


}
