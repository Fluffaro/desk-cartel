package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.services.AgentService;
import com.ticket.desk_cartel.services.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final AgentService agentService;
    private AgentRepository agentRepository;

    public NotificationController(NotificationService notificationService, AgentService agentService) {
        this.notificationService = notificationService;
        this.agentService = agentService;
    }

    @PostMapping("/notifCount/{id}")
    public ResponseEntity<?> getNotifCount(@PathVariable Long id){
        Optional<Agent> agentOpt = agentRepository.findById(id);

        Agent agentNotif = agentOpt.get();

        return ResponseEntity.ok(agentNotif.getNotifCount());
    }

    @PutMapping("/clickedNotification/{id}")
    public void clickedNotification(@PathVariable Long id){
        notificationService.clickedNotification(id);
    }

    @PostMapping("/NotificationCount/{id}")
    public ResponseEntity<?> getAgentNotificationCount(@PathVariable Long id){
        return ResponseEntity.ok(notificationService.getAgentNumbersOfNotifications(id));
    }

    @PostMapping("/Notifications/{id}")
    public ResponseEntity<?> getAgentAllNotifications(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(notificationService.getAllAgentNotification(id));
    }

}
