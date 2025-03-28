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
import java.util.Collections;

@RestController  // Use @RestController for both WebSocket and REST API
@RequestMapping("${api.chat.base-url}")
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

    // 🌍 Chat for Tickets - Only visible to client, agent, and admins
    @MessageMapping("/chat")
    public void sendMessage(@Payload ChatMessageDTO chatMessageDTO, StompHeaderAccessor accessor, Principal principal) {
        String username;
        
        // Try first to get username from Principal (most reliable)
        if (principal != null) {
            username = principal.getName();
            logger.info("📩 Got username from Principal: {}", username);
        } else {
            // Fall back to session attributes if Principal is not available
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
                username = (String) sessionAttributes.get("username");
                logger.info("📩 Got username from session attributes: {}", username);
            } else {
                // Last resort: try to get from message payload
                username = chatMessageDTO.getSenderUsername();
                logger.info("📩 Using username from message payload: {}", username);
                
                if (username == null || username.isEmpty()) {
                    logger.error("❌ No username found in any source (Principal, session, payload)");
                    throw new RuntimeException("Unauthorized WebSocket access - no username found");
                }
            }
        }
        
        // Always set the sender username in the message to ensure consistency
        chatMessageDTO.setSenderUsername(username);
        
        // Check if this is a ticket chat (should have a ticketId)
        if (chatMessageDTO.getTicketId() != null && !chatMessageDTO.getTicketId().isEmpty()) {
            String ticketId = chatMessageDTO.getTicketId();
            logger.info("📩 Processing ticket chat message for ticket {}: {}", ticketId, chatMessageDTO);
            logger.info("📩 Sender: {}, Receiver: {}, Content: {}, ClientMessageId: {}", 
                chatMessageDTO.getSenderUsername(), 
                chatMessageDTO.getReceiverUsername(), 
                chatMessageDTO.getMessageContent(),
                chatMessageDTO.getClientMessageId());
            
            // Fix special receiver format issue - if starts with TICKET_, change to PUBLIC_CHAT
            if (chatMessageDTO.getReceiverUsername() != null && 
                chatMessageDTO.getReceiverUsername().startsWith("TICKET_")) {
                logger.info("⚠️ Replacing special receiver format '{}' with 'PUBLIC_CHAT'", 
                    chatMessageDTO.getReceiverUsername());
                chatMessageDTO.setReceiverUsername("PUBLIC_CHAT");
            }
                    
            try {
                // Check for duplicate messages by clientMessageId if provided
                if (chatMessageDTO.getClientMessageId() != null && !chatMessageDTO.getClientMessageId().isEmpty()) {
                    // Try to find existing message with same clientMessageId
                    boolean isDuplicate = chatService.isClientMessageIdDuplicate(chatMessageDTO.getClientMessageId());
                    
                    if (isDuplicate) {
                        logger.warn("⚠️ Duplicate message detected with clientMessageId: {} - skipping save", 
                            chatMessageDTO.getClientMessageId());
                        
                        // Even for duplicates, we still want to broadcast the message to ensure delivery
                        // But we'll skip the database save
                        broadcastTicketMessage(chatMessageDTO, ticketId);
                        return;
                    }
                    
                    logger.info("✅ Not a duplicate message, proceeding with save");
                }
                
                // Save the message first
                ChatMessage savedMessage = null;
                try {
                    savedMessage = chatService.saveMessage(chatMessageDTO);
                    logger.info("📩 Ticket message saved with ID {}: {}", savedMessage.getId(), savedMessage.getMessageContent());
                } catch (Exception e) {
                    logger.error("❌ Failed to save ticket message: {}", e.getMessage(), e);
                    
                    if (e.getMessage().contains("Receiver not found")) {
                        logger.info("🔄 Receiver not found. Creating temporary receiver and retrying.");
                        
                        // Save a copy of the clientMessageId in case it gets overridden
                        String clientMsgId = chatMessageDTO.getClientMessageId();
                        
                        // Broadcast the message anyway so the UI gets updated
                        broadcastTicketMessage(chatMessageDTO, ticketId);
                        
                        return;
                    } else {
                        throw e; // Rethrow if it's not a receiver not found error
                    }
                }
                
                // Update the DTO with the actual ID from the saved message
                if (savedMessage != null) {
                    chatMessageDTO.setId(savedMessage.getId());
                    logger.info("📩 Updated DTO with saved message ID: {}", chatMessageDTO.getId());
                }
                
                // Handle broadcasting the message
                broadcastTicketMessage(chatMessageDTO, ticketId);
                
            } catch (Exception e) {
                logger.error("❌ Failed to save ticket message: {}", e.getMessage(), e);
                
                // Try to analyze the error
                if (e.getMessage().contains("not found")) {
                    logger.error("💡 This appears to be a user not found error. Check that both sender '{}' and receiver '{}' exist in the database.",
                        chatMessageDTO.getSenderUsername(),
                        chatMessageDTO.getReceiverUsername());
                } else if (e.getMessage().contains("constraint")) {
                    logger.error("💡 This appears to be a database constraint violation. Check field values and constraints.");
                }
                
                throw new RuntimeException("Error saving ticket chat message: " + e.getMessage());
            }
        } else {
            // Log that this message doesn't have a ticketId
            logger.warn("⚠️ Message sent without ticketId - treating as general chat message");
            
            // For public messages, set a special receiver name or use the sender as receiver
            if (chatMessageDTO.getReceiverUsername() == null || chatMessageDTO.getReceiverUsername().isEmpty()) {
                chatMessageDTO.setReceiverUsername("PUBLIC_CHAT");  // Special receiver for public messages
            }

            try {
                ChatMessage savedMessage = chatService.saveMessage(chatMessageDTO);
                logger.info("📩 General message saved: {}", savedMessage.getMessageContent());
                
                // Send to public topic
                messagingTemplate.convertAndSend("/topic/messages", chatMessageDTO);
                logger.info("✅ Sent general message to public topic");
                
            } catch (Exception e) {
                logger.error("❌ Failed to save message: {}", e.getMessage(), e);
                throw new RuntimeException("Error saving chat message: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to broadcast a ticket message to all participants
     */
    private void broadcastTicketMessage(ChatMessageDTO chatMessageDTO, String ticketId) {
        try {
            // Print the complete message being sent
            logger.info("🔎 Broadcasting message for ticket {}: {}", ticketId, chatMessageDTO);
            logger.info("🔎 Message data - Content: '{}', Sender: {}, Receiver: {}, ClientMsgId: {}", 
                chatMessageDTO.getMessageContent(),
                chatMessageDTO.getSenderUsername(),
                chatMessageDTO.getReceiverUsername(),
                chatMessageDTO.getClientMessageId());
            
            // Get all participants for this ticket conversation
            Map<String, Object> ticketChatDetails = chatService.getTicketChatDetails(ticketId);
            List<String> participants = (List<String>) ticketChatDetails.get("participants");
            
            // Debug the existing participants
            logger.info("🔍 Current participants for ticket {}: {}", ticketId, participants);
            
            // If no participants found yet (first message), create a new list
            if (participants == null || participants.isEmpty()) {
                participants = new ArrayList<>();
                logger.info("👥 No participants found for ticket {}, creating new list", ticketId);
            }
            
            // Ensure current user is in participants
            String username = chatMessageDTO.getSenderUsername();
            if (!participants.contains(username)) {
                participants.add(username);
                logger.info("👤 Added sender {} to participants list", username);
            }
            
            // Make sure required users are in participants list (admin and agent)
            Set<String> requiredParticipants = new HashSet<>();
            requiredParticipants.add("admin");
            requiredParticipants.add("neil");  // Admin user specifically 
            requiredParticipants.add("marie"); // Agent user
            
            for (String requiredUser : requiredParticipants) {
                if (!participants.contains(requiredUser)) {
                    participants.add(requiredUser);
                    logger.info("👤 Added required user {} to participants list", requiredUser);
                }
            }
            
            // Create a ticket-specific topic destination
            String ticketDestination = "/topic/ticket/" + ticketId;
            logger.info("📩 Sending ticket message to destination: {}", ticketDestination);
            
            // Send message to ticket-specific topic
            try {
                messagingTemplate.convertAndSend(
                    ticketDestination, 
                    chatMessageDTO
                );
                logger.info("✅ Successfully sent message to topic: {}", ticketDestination);
            } catch (Exception e) {
                logger.error("❌ Error sending to topic {}: {}", ticketDestination, e.getMessage(), e);
            }
            
            // Log what we're about to do
            logger.info("📩 About to send ticket message to {} individual participants", participants.size());
            
            // Also send individually to each participant to ensure they receive it
            // (this is a belt-and-suspenders approach)
            for (String participant : participants) {
                if (!participant.equals("SYSTEM") && !participant.equals("PUBLIC_CHAT")) {
                    String userQueueDestination = "/user/" + participant + "/queue/ticket/" + ticketId;
                    logger.info("📩 Sending ticket message to participant: {} via {}", 
                        participant, userQueueDestination);
                    
                    try {
                        messagingTemplate.convertAndSendToUser(
                            participant,
                            "/queue/ticket/" + ticketId,
                            chatMessageDTO
                        );
                        logger.info("✅ Successfully sent to participant: {}", participant);
                    } catch (Exception e) {
                        logger.error("❌ Error sending to participant {}: {}", participant, e.getMessage(), e);
                    }
                }
            }
            
            // Log success
            logger.info("✅ Sent ticket message to {} participants for ticket {}", 
                participants.size(), ticketId);
        } catch (Exception e) {
            logger.error("❌ Error broadcasting ticket message: {}", e.getMessage(), e);
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
    
    /**
     * Load chat history
     */
    @MessageMapping("/load-history")
    public void loadChatHistory(@Payload Map<String, Object> payload, StompHeaderAccessor accessor, Principal principal) {
        String username;
        Object ticketIdObj = payload != null ? payload.get("ticketId") : null;
        String ticketId = ticketIdObj != null ? ticketIdObj.toString() : null;
        
        logger.info("🔄 Received request to load chat history. Payload: {}", payload);
        logger.info("🎫 Ticket ID parameter: {}", ticketId);
        
        // Check for requestId to prevent duplicate processing
        String requestId = payload != null && payload.get("requestId") != null 
            ? payload.get("requestId").toString() 
            : null;
            
        // Use a static Set to track recently processed request IDs
        if (requestId != null) {
            // Use a synchronized block to prevent concurrent modification
            synchronized (PROCESSED_REQUEST_IDS) {
                if (PROCESSED_REQUEST_IDS.contains(requestId)) {
                    logger.info("⚠️ Duplicate history request detected with ID {}, skipping", requestId);
                    return;
                }
                
                // Add to processed set and schedule removal after 10 seconds
                PROCESSED_REQUEST_IDS.add(requestId);
                
                // Clean up old request IDs periodically to prevent memory leaks
                new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            synchronized (PROCESSED_REQUEST_IDS) {
                                PROCESSED_REQUEST_IDS.remove(requestId);
                            }
                        }
                    }, 
                    10000 // 10 seconds
                );
            }
        }
        
        // Get username (from Principal or session)
        if (principal != null) {
            username = principal.getName();
            logger.info("✅ Using username from Principal: {}", username);
        } else {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
                username = (String) sessionAttributes.get("username");
                logger.info("✅ Using username from session attributes: {}", username);
            } else {
                logger.error("❌ No username found in Principal or session attributes when loading chat history");
                messagingTemplate.convertAndSendToUser(
                    accessor.getSessionId(),
                    "/queue/errors",
                    Map.of("error", "Unauthorized access when loading chat history")
                );
                return;
            }
        }
        
        logger.info("🔄 Loading chat history for user: {} with ticket ID: {}", username, ticketId);
        
        try {
            // If ticketId is provided, get messages for that specific ticket
            if (ticketId != null && !ticketId.isEmpty()) {
                logger.info("📄 Getting messages for ticket ID: {}", ticketId);
                
                // TODO: Add role-based validation here (client/agent assigned to ticket or admin)
                
                // Get ticket chat details with participant info using the new method
                Map<String, Object> ticketChatDetails = chatService.getTicketChatDetails(ticketId);
                
                // Send ticket-specific chat history with additional metadata
                Map<String, Object> response = Map.of(
                    "type", "ticket",
                    "ticketId", ticketId,
                    "messages", ticketChatDetails.get("messages"),
                    "participants", ticketChatDetails.get("participants"),
                    "clientUsername", ticketChatDetails.containsKey("clientUsername") ? 
                                    ticketChatDetails.get("clientUsername") : "",
                    "agentUsername", ticketChatDetails.containsKey("agentUsername") ? 
                                    ticketChatDetails.get("agentUsername") : "",
                    "messageCount", ticketChatDetails.get("messageCount"),
                    "requestId", requestId != null ? requestId : "" // Add requestId to response for tracking
                );
                
                // Debug what we're about to send
                logger.info("🔍 SENDING HISTORY: type=ticket, ticketId={}, messageCount={}, to user={}", 
                    ticketId, ticketChatDetails.get("messageCount"), username);
                
                // Debug the exact destination we're sending to
                String destination = "/user/" + username + "/queue/chat-history";
                logger.info("📨 Sending to destination: {}", destination);
                
                if (ticketChatDetails.get("messages") != null) {
                    List<?> messages = (List<?>) ticketChatDetails.get("messages");
                    if (!messages.isEmpty()) {
                        logger.info("📝 First message sample: {}", messages.get(0));
                        if (messages.size() > 1) {
                            logger.info("📝 Last message sample: {}", messages.get(messages.size() - 1));
                        }
                    }
                }
                
                try {
                    // Send using the standard simpMessagingTemplate approach
                    messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/chat-history",
                        response
                    );
                    
                    logger.info("✅ Sent ticket chat details via convertAndSendToUser");
                    
                    // Also try sending directly to the full destination path as a fallback
                    // This ensures compatibility with different STOMP client implementations
                    messagingTemplate.convertAndSend(
                        "/topic/chat-history/" + ticketId,
                        response
                    );
                    
                    logger.info("✅ Also sent ticket chat details via direct topic");
                    
                    // Add a third fallback using session ID-based addressing, which is most reliable
                    // when there are connection issues
                    String sessionId = accessor.getSessionId();
                    if (sessionId != null) {
                        messagingTemplate.convertAndSendToUser(
                            sessionId,
                            "/queue/chat-history",
                            response
                        );
                        logger.info("✅ Also sent ticket chat history via session ID: {}", sessionId);
                        
                        // Try an additional direct queue destination as a last resort
                        messagingTemplate.convertAndSend(
                            "/queue/chat-history-user" + sessionId,
                            response
                        );
                        logger.info("✅ Also sent ticket chat history via direct queue with session ID");
                    }
                } catch (Exception e) {
                    logger.error("❌ Error sending chat history: {}", e.getMessage(), e);
                }
                
                logger.info("✅ Sent ticket chat details for ticket ID {} to {}", ticketId, username);
            } else {
                // This is a request for general chat history
                
                // For admins, we might want to send a summary of recent chats
                // For agents, we might want to send only their assigned tickets' chats
                // For clients, we might want to send only their tickets' chats
                
                // For now, continue with the existing implementation but log that this might need to change
                logger.info("📝 Loading general chat history - this should ideally be restricted based on user roles");
                
                // Send recent public messages as fallback
                List<ChatMessage> publicMessages = chatService.getRecentPublicMessages();
                List<ChatMessageDTO> publicDtos = chatService.convertToDTO(publicMessages);
                
                logger.info("📝 Found {} public messages to send", publicDtos.size());
                
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
            }
        } catch (Exception e) {
            logger.error("❌ Error loading chat history: {}", e.getMessage(), e);
            
            // Send error notification to the client
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                Map.of(
                    "error", "Failed to load chat history: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                )
            );
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

    // Add a static set to track processed request IDs to prevent duplicate processing
    private static final Set<String> PROCESSED_REQUEST_IDS = Collections.synchronizedSet(new HashSet<>());
}
