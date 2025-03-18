package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create a notification when a ticket is created
     */
    public Notification notifyTicketCreated(Ticket ticket) {
        User owner = ticket.getTicketOwner();
        
        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setTitle("Ticket Created");
        notification.setMessage("Your ticket '" + ticket.getTitle() + "' has been created successfully.");
        notification.setType(NotificationType.TICKET_CREATED);
        notification.setTicket(ticket);
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Create a notification when a ticket is assigned to an agent
     */
    public Notification notifyTicketAssigned(Ticket ticket) {
        Agent agent = ticket.getAssignedTicket();
        if (agent == null) {
            return null;
        }
        
        // Get the agent's user account
        User agentUser = agent.getUser();
        
        Notification notification = new Notification();
        notification.setUser(agentUser);
        notification.setTitle("New Ticket Assigned");
        notification.setMessage("Ticket '" + ticket.getTitle() + "' has been assigned to you.");
        notification.setType(NotificationType.TICKET_ASSIGNED);
        notification.setTicket(ticket);
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Create a deadline warning notification for a ticket
     * This is a placeholder implementation with hardcoded values
     */
    public Notification notifyDeadlineWarning(Ticket ticket) {
        Agent agent = ticket.getAssignedTicket();
        if (agent == null) {
            return null;
        }
        
        User agentUser = agent.getUser();
        
        // Hardcoded deadline logic based on priority
        // In the future, this would use a more sophisticated calculation
        int deadlineHours;
        if (ticket.getPriority() == null || ticket.getPriority() == Priority.NOT_ASSIGNED) {
            return null; // No deadline for unassigned priority
        }
        
        switch (ticket.getPriority()) {
            case LOW:
                deadlineHours = 4;
                break;
            case MEDIUM:
                deadlineHours = 8;
                break;
            case HIGH:
                deadlineHours = 10;
                break;
            case CRITICAL:
                deadlineHours = 12;
                break;
            default:
                return null;
        }
        
        // Check if the ticket is approaching its deadline (75% of time elapsed)
        LocalDateTime deadline = ticket.getDate_started().plusHours(deadlineHours);
        LocalDateTime warningTime = ticket.getDate_started().plusHours((long) (deadlineHours * 0.75));
        
        if (LocalDateTime.now().isAfter(warningTime) && LocalDateTime.now().isBefore(deadline)) {
            Notification notification = new Notification();
            notification.setUser(agentUser);
            notification.setTitle("Deadline Approaching");
            notification.setMessage("Ticket '" + ticket.getTitle() + "' is approaching its deadline. Please complete it soon.");
            notification.setType(NotificationType.TICKET_DEADLINE_WARNING);
            notification.setTicket(ticket);
            
            return notificationRepository.save(notification);
        }
        
        return null;
    }
    
    /**
     * Mark a notification as read
     */
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
    
    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
    }
    
    /**
     * Count unread notifications for a user
     */
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }
}