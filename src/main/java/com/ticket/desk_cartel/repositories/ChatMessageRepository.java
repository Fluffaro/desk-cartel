package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "JOIN FETCH m.receiver " +
            "WHERE (m.sender.username = :sender AND m.receiver.username = :receiver) " +
            "OR (m.sender.username = :receiver AND m.receiver.username = :sender) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistory(String sender, String receiver);
}
