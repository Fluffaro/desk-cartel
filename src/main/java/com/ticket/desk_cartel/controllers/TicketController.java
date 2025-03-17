package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.TicketDTO;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.services.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
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
