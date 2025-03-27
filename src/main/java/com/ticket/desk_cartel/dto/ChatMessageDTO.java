package com.ticket.desk_cartel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    private Long id;
    private String senderUsername;
    private String receiverUsername;
    private String messageContent;
    private String timestamp;
    
    // Added fields to match entity
    private String ticketId;
    private String clientMessageId;
    
    // Optional fields for additional metadata
    private Boolean isRead;
    private Boolean isSystem;
    
    // Convenience method to create a system message
    public static ChatMessageDTO createSystemMessage(String content, String receiverUsername) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setSenderUsername("SYSTEM");
        dto.setReceiverUsername(receiverUsername);
        dto.setMessageContent(content);
        dto.setTimestamp(LocalDateTime.now().toString());
        dto.setIsSystem(true);
        return dto;
    }
}
