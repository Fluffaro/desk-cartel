package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender s " +
            "JOIN FETCH m.receiver r " +
            "WHERE (s.username = :sender AND r.username = :receiver) " +
            "OR (s.username = :receiver AND r.username = :sender) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistory(String sender, String receiver);
    
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "JOIN FETCH m.receiver " +
            "WHERE m.receiver.username = 'PUBLIC_CHAT' " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findPublicMessages();
    
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "JOIN FETCH m.receiver " +
            "WHERE m.receiver.username = 'PUBLIC_CHAT' " +
            "ORDER BY m.timestamp DESC " +
            "LIMIT 50")
    List<ChatMessage> findRecentPublicMessages();
    
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender s " +
           "JOIN FETCH m.receiver r " +
           "WHERE s.username = :username OR r.username = :username " +
           "ORDER BY m.timestamp DESC")
    List<ChatMessage> findAllBySenderUsernameOrReceiverUsername(String username, String username2);
    
    // Debug query to find all messages for a specific user
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender s " +
           "JOIN FETCH m.receiver r " +
           "WHERE s.username = :username " +
           "ORDER BY m.timestamp DESC")
    List<ChatMessage> findAllSentByUsername(String username);
    
    // Debug query to find all messages received by a specific user
    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender s " +
           "JOIN FETCH m.receiver r " +
           "WHERE r.username = :username " +
           "ORDER BY m.timestamp DESC")
    List<ChatMessage> findAllReceivedByUsername(String username);
}
