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
import com.ticket.desk_cartel.security.JwtUtil;
import com.ticket.desk_cartel.configs.WebSocketEventListener;
import java.security.Principal;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.time.LocalDateTime;

@RestController  // Use @RestController for both WebSocket and REST API
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")  // Allow frontend access
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtUtil jwtUtil;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate, JwtUtil jwtUtil) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.jwtUtil = jwtUtil;
    }

    // 🌍 Public Chat (Broadcast Messages)
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO chatMessageDTO, StompHeaderAccessor accessor, Principal principal) {
        String username;
        
        // Try first to get username from Principal (most reliable)
        if (principal != null) {
            username = principal.getName();
            logger.info("📩 Got username from Principal: {}", username);
        } else {
            // Fall back to session attributes if Principal is not available
            username = (String) accessor.getSessionAttributes().get("username");
            logger.info("📩 Got username from session attributes: {}", username);
        }
        
        if (username == null) {
            logger.error("❌ No username found in Principal or session attributes");
            throw new RuntimeException("Unauthorized WebSocket access");
        }

        chatMessageDTO.setSenderUsername(username);
        
        // For public messages, set a special receiver name or use the sender as receiver
        if (chatMessageDTO.getReceiverUsername() == null || chatMessageDTO.getReceiverUsername().isEmpty()) {
            chatMessageDTO.setReceiverUsername("PUBLIC_CHAT");  // Special receiver for public messages
        }

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
    public void sendPrivateMessage(@Payload ChatMessageDTO chatMessageDTO, StompHeaderAccessor accessor, Principal principal) {
        String username;
        
        // Try first to get username from Principal (most reliable)
        if (principal != null) {
            username = principal.getName();
            logger.info("📨 Got username from Principal: {}", username);
        } else {
            // Fall back to session attributes if Principal is not available
            username = (String) accessor.getSessionAttributes().get("username");
            logger.info("📨 Got username from session attributes: {}", username);
        }
        
        if (username == null) {
            logger.error("❌ No username found in Principal or session attributes");
            throw new RuntimeException("Unauthorized WebSocket access");
        }

        chatMessageDTO.setSenderUsername(username);
        logger.info("📨 Processing private message from {} to {}", username, chatMessageDTO.getReceiverUsername());
        
        // Debug session attributes
        logger.info("📨 WebSocket session ID: {}", accessor.getSessionId());
        logger.info("📨 Session attributes: {}", accessor.getSessionAttributes());
        
        // Check if recipient is connected
        String receiverUsername = chatMessageDTO.getReceiverUsername();
        boolean isReceiverOnline = WebSocketEventListener.isUserConnected(receiverUsername);
        logger.info("📨 Is recipient {} online? {}", receiverUsername, isReceiverOnline);

        try {
            chatService.saveMessage(chatMessageDTO);
            
            if (!isReceiverOnline) {
                logger.warn("⚠️ Receiver {} is not currently connected, message will be delivered when they reconnect", receiverUsername);
            }
            
            // Debug destination construction
            String destination = "/user/" + chatMessageDTO.getReceiverUsername() + "/queue/private-messages";
            logger.info("📨 Sending private message to destination: {}", destination);
            
            // Debug actual payload being sent
            logger.info("📨 Private message payload: {}", chatMessageDTO);
            
            // Send message to user
            messagingTemplate.convertAndSendToUser(
                    receiverUsername, 
                    "/queue/private-messages", 
                    chatMessageDTO
            );
            
            // Also send a confirmation back to the sender
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/sent-confirmation",
                    Map.of(
                        "status", "sent",
                        "timestamp", System.currentTimeMillis(),
                        "recipient", receiverUsername, 
                        "messageContent", chatMessageDTO.getMessageContent()
                    )
            );
            
            logger.info("📨 Private message sent to {}", receiverUsername);
        } catch (Exception e) {
            logger.error("❌ Failed to send private message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send private message: " + e.getMessage());
        }
    }

    // 📜 Fetch Chat History (REST API)
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @RequestParam String sender, @RequestParam String receiver) {
        try {
            List<ChatMessage> chatHistory = chatService.getChatHistory(sender, receiver);
            List<ChatMessageDTO> dtos = chatService.convertToDTO(chatHistory);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("❌ Error retrieving chat history: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    // 🌐 Fetch Public Chat Messages (REST API)
    @GetMapping("/public-messages")
    public ResponseEntity<List<ChatMessageDTO>> getPublicMessages() {
        try {
            List<ChatMessage> messages = chatService.getAllPublicMessages();
            List<ChatMessageDTO> dtos = chatService.convertToDTO(messages);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("❌ Error retrieving public messages: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    // 🌐 Fetch Recent Public Chat Messages (REST API)
    @GetMapping("/public-messages/recent")
    public ResponseEntity<List<ChatMessageDTO>> getRecentPublicMessages() {
        try {
            List<ChatMessage> messages = chatService.getRecentPublicMessages();
            List<ChatMessageDTO> dtos = chatService.convertToDTO(messages);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("❌ Error retrieving recent public messages: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    // 👥 Get list of users that the current user has chatted with
    @GetMapping("/conversation-partners")
    public ResponseEntity<List<String>> getConversationPartners(@RequestParam String username) {
        try {
            List<String> partners = chatService.getUserConversationPartners(username);
            return ResponseEntity.ok(partners);
        } catch (Exception e) {
            logger.error("❌ Error retrieving conversation partners: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    // 🧑‍💻 Load Chat History (WebSocket endpoint)
    @MessageMapping("/load-history")
    public void loadChatHistory(StompHeaderAccessor accessor, Principal principal) {
        String username;
        
        // Get username (from Principal or session)
        if (principal != null) {
            username = principal.getName();
        } else {
            username = (String) accessor.getSessionAttributes().get("username");
        }
        
        if (username == null) {
            logger.error("❌ No username found when loading chat history");
            messagingTemplate.convertAndSendToUser(
                accessor.getSessionId(),
                "/queue/errors",
                Map.of("error", "Unauthorized access when loading chat history")
            );
            return;
        }
        
        logger.info("🔄 Loading chat history for user: {}", username);
        
        try {
            // Send recent public messages
            List<ChatMessage> publicMessages = chatService.getRecentPublicMessages();
            List<ChatMessageDTO> publicDtos = chatService.convertToDTO(publicMessages);
            
            // Send message history to user
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/chat-history",
                Map.of(
                    "type", "public",
                    "messages", publicDtos
                )
            );
            
            logger.info("✅ Sent {} public messages to {}", publicDtos.size(), username);
            
            // Find users who have had conversations with this user
            List<String> conversationPartners = chatService.getUserConversationPartners(username);
            
            // If we don't have any conversation partners yet, we'll add some defaults for new users
            if (conversationPartners.isEmpty()) {
                Set<String> defaultPartners = new HashSet<>();
                defaultPartners.add("admin");
                defaultPartners.add("support");
                defaultPartners.add("test");
                conversationPartners = new ArrayList<>(defaultPartners);
            }
            
            // Send the list of conversation partners to the client
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/conversation-partners",
                Map.of(
                    "partners", conversationPartners
                )
            );
            
            logger.info("✅ Sent {} conversation partners to {}", conversationPartners.size(), username);
            
            // For each user, load private chat history and send it
            for (String otherUser : conversationPartners) {
                if (!otherUser.equals(username) && !otherUser.equals("PUBLIC_CHAT")) {
                    List<ChatMessage> privateMessages = chatService.getChatHistory(username, otherUser);
                    
                    // Only send if there are messages
                    if (privateMessages != null && !privateMessages.isEmpty()) {
                        List<ChatMessageDTO> privateDtos = chatService.convertToDTO(privateMessages);
                        
                        messagingTemplate.convertAndSendToUser(
                            username,
                            "/queue/chat-history",
                            Map.of(
                                "type", "private",
                                "otherUser", otherUser,
                                "messages", privateDtos
                            )
                        );
                        
                        logger.info("✅ Sent {} private messages with {} to {}", 
                            privateDtos.size(), otherUser, username);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("❌ Error loading chat history: {}", e.getMessage(), e);
        }
    }

    // 🔑 Test Endpoint to generate a JWT for WebSocket testing
    @GetMapping("/test-token/{username}")
    public ResponseEntity<String> generateTestToken(@PathVariable String username) {
        try {
            // Generate token with a default "USER" role for testing
            String token = jwtUtil.generateToken(username, "USER");
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            logger.error("❌ Error generating test token: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // 🔍 Debug endpoint to check active WebSocket sessions
    @GetMapping("/debug/active-sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("activeSessions", WebSocketEventListener.getActiveSessionsInfo());
            debugInfo.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("❌ Error retrieving active sessions: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // 🔍 Debug endpoint to test sending private messages
    @GetMapping("/debug/test-private-message")
    public ResponseEntity<Map<String, Object>> testPrivateMessage(
            @RequestParam String sender,
            @RequestParam String receiver,
            @RequestParam String message) {
        try {
            logger.info("🔍 Testing private message from {} to {}: {}", sender, receiver, message);
            
            // Create a test message
            ChatMessageDTO testMessage = new ChatMessageDTO();
            testMessage.setSenderUsername(sender);
            testMessage.setReceiverUsername(receiver);
            testMessage.setMessageContent(message);
            
            // Send via direct method
            messagingTemplate.convertAndSendToUser(
                    receiver,
                    "/queue/private-messages",
                    testMessage
            );
            
            // Send confirmation to the sender
            messagingTemplate.convertAndSendToUser(
                    sender,
                    "/queue/sent-confirmation",
                    Map.of(
                        "status", "sent",
                        "timestamp", System.currentTimeMillis(),
                        "recipient", receiver,
                        "messageContent", message
                    )
            );
            
            logger.info("✅ Test message sent successfully");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test message sent");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error sending test message: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    // 🔍 Debug endpoint to check messages for a specific user
    @GetMapping("/debug/user-messages")
    public ResponseEntity<Map<String, Object>> getUserMessages(@RequestParam String username) {
        try {
            logger.info("🔍 Checking all messages for user: {}", username);
            
            // Get all sent and received messages
            List<ChatMessage> sentMessages = chatService.getAllMessagesSentByUser(username);
            List<ChatMessage> receivedMessages = chatService.getAllMessagesReceivedByUser(username);
            
            // Get conversation partners
            List<String> partners = chatService.getUserConversationPartners(username);
            
            // For each partner, get the conversation history
            Map<String, List<ChatMessageDTO>> conversations = new HashMap<>();
            for (String partner : partners) {
                List<ChatMessage> history = chatService.getChatHistory(username, partner);
                List<ChatMessageDTO> dtos = chatService.convertToDTO(history);
                conversations.put(partner, dtos);
            }
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("username", username);
            debugInfo.put("sentCount", sentMessages.size());
            debugInfo.put("receivedCount", receivedMessages.size());
            debugInfo.put("partners", partners);
            debugInfo.put("conversations", conversations);
            debugInfo.put("timestamp", LocalDateTime.now());
            
            logger.info("✅ Successfully retrieved debug info for {}", username);
            logger.info("📊 Stats: {} sent messages, {} received messages, {} conversation partners", 
                sentMessages.size(), receivedMessages.size(), partners.size());
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("❌ Error retrieving user messages: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
