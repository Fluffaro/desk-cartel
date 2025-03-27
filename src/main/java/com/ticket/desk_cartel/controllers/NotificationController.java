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
    private final AgentRepository agentRepository;

    public NotificationController(NotificationService notificationService, AgentService agentService, AgentRepository agentRepository) {
        this.notificationService = notificationService;
        this.agentService = agentService;
        this.agentRepository = agentRepository;
    }

    @GetMapping("/notifCount/{id}")
    public ResponseEntity<?> getNotifCount(@PathVariable Long id){
        Optional<Agent> agentOpt = agentRepository.findById(id);

        Agent agentNotif = agentOpt.get();

        return ResponseEntity.ok(agentNotif.getNotifCount());
    }

    @PutMapping("/clickedNotification")
    public void agentClickedNotification(@RequestParam Long id){
        notificationService.clickedAgentNotification(id);
    }

    @PutMapping("/clickedNotification/{id}")
    public void userClickedNotification(@PathVariable Long id){
        notificationService.clickedUserNotification(id);
    }

    @GetMapping("/NotificationCount")
    public ResponseEntity<?> getAgentNotificationCount(@RequestParam Long id){
        return ResponseEntity.ok(notificationService.getAgentNumbersOfNotifications(id));
    }

    @GetMapping("/Notifications")
    public ResponseEntity<?> getAgentAllNotifications(@RequestParam Long id) throws Exception {
        return ResponseEntity.ok(notificationService.getAllAgentNotification(id));
    }

    @GetMapping("/NotificationCount/{id}")
    public ResponseEntity<?> getUserNotificationCount(@PathVariable Long id){
        return ResponseEntity.ok(notificationService.getUserNumbersOfNotifications(id));
    }

    @GetMapping("/Notifications/{id}")
    public ResponseEntity<?> getUserAllNotifications(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(notificationService.getAllUserNotification(id));
    }

}
