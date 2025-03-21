package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByAssignedTicket_Id(Long assignedAgent);
}
