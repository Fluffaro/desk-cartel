package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AgentRepository agentRepository;

    public void clickedNotification(Long id){
        Optional<Agent> agentOpt = agentRepository.findById(id);

        Agent agent = agentOpt.get();

        agent.setNotifCount(0);
    }

    public int getNumbersOfNotifications(Long id){
        Optional<Agent> agentOpt = agentRepository.findById(id);

        Agent agent = agentOpt.get();

        return agent.getNotifCount();
    }

    public List<Notification> getAllAgentNotification(Long id) throws Exception {
        Optional<Notification> notificationOpt = notificationRepository.findByAssignedTicket_Id(id);
        if(notificationOpt.isEmpty()){
            throw new Exception("Notification Empty");
        }
        Notification notification = notificationOpt.get();

        return (List<Notification>) notification;
    }
}
