package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.Notification;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.services.NotificationService;
import com.ticket.desk_cartel.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * Get all notifications for the current user
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    /**
     * Get unread notifications for the current user
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user));
    }

    /**
     * Count unread notifications for the current user
     */
    @GetMapping("/count-unread")
    public ResponseEntity<Map<String, Long>> countUnreadNotifications(Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        long count = notificationService.countUnreadNotifications(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read
     */
    @PatchMapping("/{id}/mark-read")
    public ResponseEntity<Notification> markNotificationAsRead(
            @PathVariable("id") Long notificationId,
            Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Notification notification = notificationService.markAsRead(notificationId);
        
        // Security check to ensure users can only mark their own notifications as read
        if (!notification.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(notification);
    }
}