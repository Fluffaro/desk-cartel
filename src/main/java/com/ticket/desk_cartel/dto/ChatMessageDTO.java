package com.ticket.desk_cartel.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageDTO {
    private String senderUsername;
    private String receiverUsername;
    private String messageContent;
    private LocalDateTime timestamp;
}
