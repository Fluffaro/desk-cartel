package com.ticket.desk_cartel.configs;

import com.ticket.desk_cartel.services.TicketService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final TicketService ticketService;

    public SchedulingConfig(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Check for tickets approaching deadlines every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    public void checkTicketDeadlines() {
        ticketService.checkTicketDeadlines();
    }
} 