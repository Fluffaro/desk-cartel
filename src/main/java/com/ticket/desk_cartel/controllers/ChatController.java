package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.ChatMessageDTO;
import com.ticket.desk_cartel.entities.ChatMessage;
import com.ticket.desk_cartel.services.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController  // Use @RestController for both WebSocket and REST API
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")  // Allow frontend access
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    // 🌍 Public Chat (Broadcast Messages)
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO chatMessageDTO, StompHeaderAccessor accessor) {
        String username = (String) accessor.getSessionAttributes().get("username");
        if (username == null) {
            throw new RuntimeException("Unauthorized WebSocket access");
        }

        chatMessageDTO.setSenderUsername(username);

        try {
            ChatMessage savedMessage = chatService.saveMessage(chatMessageDTO);
            logger.info("📩 Message saved: {}", savedMessage.getMessageContent());
            return chatMessageDTO;
        } catch (Exception e) {
            logger.error("❌ Failed to save message: {}", e.getMessage());
            throw new RuntimeException("Error saving chat message");
        }
    }

    // 🔒 Private Chat (Direct Messaging)
    @MessageMapping("/private-chat")
    public void sendPrivateMessage(@Payload ChatMessageDTO chatMessageDTO, StompHeaderAccessor accessor) {
        String username = (String) accessor.getSessionAttributes().get("username");
        if (username == null) {
            throw new RuntimeException("Unauthorized WebSocket access");
        }

        chatMessageDTO.setSenderUsername(username);

        try {
            chatService.saveMessage(chatMessageDTO);
            messagingTemplate.convertAndSendToUser(
                    chatMessageDTO.getReceiverUsername(), "/queue/private-messages", chatMessageDTO
            );
            logger.info("📨 Private message sent to {}", chatMessageDTO.getReceiverUsername());
        } catch (Exception e) {
            logger.error("❌ Failed to send private message: {}", e.getMessage());
        }
    }

    // 📜 Fetch Chat History (REST API)
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @RequestParam String sender, @RequestParam String receiver) {
        try {
            List<ChatMessage> chatHistory = chatService.getChatHistory(sender, receiver);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            logger.error("❌ Error retrieving chat history: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
