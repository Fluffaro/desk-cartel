package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
