package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.NotificationRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private UserRepository userRepository;

    /**
     * Reset the notification count for an agent
     * 
     * @param id Agent ID
     */
    @Transactional
    public void clickedAgentNotification(Long id) {
        Optional<Agent> agentOpt = agentRepository.findById(id);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setNotifCount(0);
            agentRepository.save(agent);
        } else {
            logger.warn("Cannot reset notifications for non-existent agent: {}", id);
        }
    }

    /**
     * Reset the notification count for a user
     * 
     * @param id User ID
     */
    @Transactional
    public void clickedUserNotification(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setNotifCount(0);
            userRepository.save(user);
        } else {
            logger.warn("Cannot reset notifications for non-existent user: {}", id);
        }
    }

    /**
     * Get the number of notifications for an agent
     * 
     * @param id Agent ID
     * @return Number of notifications or 0 if agent not found
     */
    public int getAgentNumbersOfNotifications(Long id) {
        Optional<Agent> agentOpt = agentRepository.findById(id);
        return agentOpt.map(Agent::getNotifCount).orElse(0);
    }

    /**
     * Get the number of notifications for a user
     * 
     * @param id User ID
     * @return Number of notifications or 0 if user not found
     */
    public int getUserNumbersOfNotifications(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(User::getNotifCount).orElse(0);
    }

    /**
     * Get all notifications for an agent
     * 
     * @param id Agent ID
     * @return List of notifications
     * @throws Exception if no notifications found
     */
    public List<Notification> getAllAgentNotification(Long id) throws Exception {
        List<Notification> notification = notificationRepository.findByAssignedTicket_Id(id);
        if (notification.isEmpty()) {
            throw new Exception("Notification Empty");
        }

        return notification;
    }

    /**
     * Get all notifications for a user
     * 
     * @param id User ID
     * @return List of notifications
     * @throws Exception if no notifications found
     */
    public List<Notification> getAllUserNotification(Long id) throws Exception {
        List<Notification> notification = notificationRepository.findByTicketCreator_Id(id);
        if (notification.isEmpty()) {
            throw new Exception("Notification Empty");
        }

        return notification;
    }
    
    /**
     * Create a notification for a ticket assignment to an agent
     * This notifies the agent that a new ticket has been assigned to them
     * 
     * @param ticket The assigned ticket
     * @return The created notification
     */
    @Transactional
    public Notification createTicketAssignedNotification(Ticket ticket) {
        if (ticket == null || ticket.getAssignedTicket() == null) {
            logger.warn("Cannot create assignment notification for null ticket or agent");
            return null;
        }
        
        Notification notification = new Notification();
        notification.setTitle("New Ticket Assigned: " + ticket.getTitle());
        notification.setDescription("You've been assigned ticket #" + ticket.getTicketId() + 
                " with " + ticket.getPriority().getName() + " priority.");
        notification.setTicket(ticket);
        notification.setAssignedTicket(ticket.getAssignedTicket());
        notification.setTicketCreator(ticket.getTicketOwner());
        
        // Increment agent notification count
        Optional<Agent> agentOpt = agentRepository.findById(ticket.getAssignedTicket().getId());
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setNotifCount(agent.getNotifCount() + 1);
            agentRepository.save(agent);
        }
        
        logger.info("Created ticket assignment notification for agent {}", ticket.getAssignedTicket().getId());
        return notificationRepository.save(notification);
    }
    
    /**
     * Create a notification for when an agent starts working on a ticket
     * This notifies the user that work has begun on their ticket
     * 
     * @param ticket The started ticket
     * @return The created notification
     */
    @Transactional
    public Notification createTicketStartedNotification(Ticket ticket) {
        if (ticket == null || ticket.getTicketOwner() == null) {
            logger.warn("Cannot create started notification for null ticket or owner");
            return null;
        }
        
        String agentInfo = ticket.getAssignedTicket() != null ? 
                "Agent " + ticket.getAssignedTicket().getUser().getUsername() : "An agent";
        
        Notification notification = new Notification();
        notification.setTitle("Work Started on Ticket: " + ticket.getTitle());
        notification.setDescription(agentInfo + " has started working on your ticket #" + 
                ticket.getTicketId() + ". Expected completion by: " + 
                ticket.getExpected_completion_date());
        notification.setTicket(ticket);
        notification.setAssignedTicket(ticket.getAssignedTicket());
        notification.setTicketCreator(ticket.getTicketOwner());
        
        // Increment user notification count
        Optional<User> userOpt = userRepository.findById(ticket.getTicketOwner().getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setNotifCount(user.getNotifCount() + 1);
            userRepository.save(user);
        }
        
        logger.info("Created ticket started notification for user {}", ticket.getTicketOwner().getId());
        return notificationRepository.save(notification);
    }
    
    /**
     * Create a notification for when a user completes a ticket
     * This notifies the agent that the client has marked the ticket as complete
     * 
     * @param ticket The completed ticket
     * @return The created notification
     */
    @Transactional
    public Notification createTicketCompletedByUserNotification(Ticket ticket) {
        if (ticket == null || ticket.getAssignedTicket() == null) {
            logger.warn("Cannot create completion notification for null ticket or agent");
            return null;
        }
        
        Notification notification = new Notification();
        notification.setTitle("Ticket Completed by User: " + ticket.getTitle());
        notification.setDescription("Ticket #" + ticket.getTicketId() + " has been marked as completed " +
                "by the user. Performance points earned: " + ticket.getPoints());
        notification.setTicket(ticket);
        notification.setAssignedTicket(ticket.getAssignedTicket());
        notification.setTicketCreator(ticket.getTicketOwner());
        
        // Increment agent notification count
        Optional<Agent> agentOpt = agentRepository.findById(ticket.getAssignedTicket().getId());
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setNotifCount(agent.getNotifCount() + 1);
            agentRepository.save(agent);
        }
        
        logger.info("Created ticket completion notification for agent {}", ticket.getAssignedTicket().getId());
        return notificationRepository.save(notification);
    }
    
    /**
     * Create a notification when no agent is available for a ticket
     * This is only sent once when the ticket status is changed to NO_AGENT_AVAILABLE
     * 
     * @param ticket The unassigned ticket
     * @return The created notification
     */
    @Transactional
    public Notification createNoAgentAvailableNotification(Ticket ticket) {
        if (ticket == null || ticket.getTicketOwner() == null) {
            logger.warn("Cannot create no-agent notification for null ticket or owner");
            return null;
        }
        
        Notification notification = new Notification();
        notification.setTitle("No Agent Available for Ticket: " + ticket.getTitle());
        notification.setDescription("Your ticket #" + ticket.getTicketId() + 
                " could not be assigned to an agent at this time. " +
                "We will automatically assign it when an agent becomes available.");
        notification.setTicket(ticket);
        notification.setTicketCreator(ticket.getTicketOwner());
        
        // Increment user notification count
        Optional<User> userOpt = userRepository.findById(ticket.getTicketOwner().getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setNotifCount(user.getNotifCount() + 1);
            userRepository.save(user);
        }
        
        logger.info("Created no-agent-available notification for user {}", ticket.getTicketOwner().getId());
        return notificationRepository.save(notification);
    }

    /**
     * get notifications of agent based on their userId
     */
    public List<Notification> getNotificationsByUserId(Long userId) throws Exception {
        Optional<Agent> agentOpt = agentRepository.findByUserId(userId);
        if (agentOpt.isEmpty()) {
            throw new Exception("No agent found for the given userId.");
        }
        return getAllAgentNotification(agentOpt.get().getId());
    }



}
