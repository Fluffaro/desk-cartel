package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.dto.ChatMessageDTO;
import com.ticket.desk_cartel.entities.ChatMessage;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.ChatMessageRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save a new chat message
     * @param chatMessageDTO The message to save
     * @return The saved message entity
     * @throws RuntimeException if sender or receiver not found
     */
    @Transactional
    public ChatMessage saveMessage(ChatMessageDTO chatMessageDTO) {
        try {
            logger.info("🔄 Starting to save message from '{}' to '{}'", 
                chatMessageDTO.getSenderUsername(), chatMessageDTO.getReceiverUsername());
            
            // No need to ensure users exist here - they're created at app startup
            
            // Get the sender and receiver from the database
            User sender = userRepository.findByUsername(chatMessageDTO.getSenderUsername())
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + chatMessageDTO.getSenderUsername()));
            
            User receiver = userRepository.findByUsername(chatMessageDTO.getReceiverUsername())
                    .orElseThrow(() -> new RuntimeException("Receiver not found: " + chatMessageDTO.getReceiverUsername()));
            
            logger.info("✅ Found both sender (ID:{}) and receiver (ID:{}) in database", 
                sender.getId(), receiver.getId());
            
            // Create a new message entity
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSender(sender);
            chatMessage.setReceiver(receiver);
            chatMessage.setMessageContent(chatMessageDTO.getMessageContent());
            chatMessage.setTimestamp(LocalDateTime.now());
            
            // Set ticketId if provided
            if (chatMessageDTO.getTicketId() != null && !chatMessageDTO.getTicketId().isEmpty()) {
                logger.info("📝 Setting ticketId {} for message", chatMessageDTO.getTicketId());
                chatMessage.setTicketId(chatMessageDTO.getTicketId());
            }
            
            // Set clientMessageId if provided (for deduplication)
            if (chatMessageDTO.getClientMessageId() != null && !chatMessageDTO.getClientMessageId().isEmpty()) {
                logger.info("📝 Setting clientMessageId {} for message", chatMessageDTO.getClientMessageId());
                chatMessage.setClientMessageId(chatMessageDTO.getClientMessageId());
            }
            
            // Save the message
            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
            logger.info("✅ Message saved with ID {}: from {} to {}", 
                      savedMessage.getId(), sender.getUsername(), receiver.getUsername());
            
            return savedMessage;
        } catch (Exception e) {
            logger.error("❌ Error saving message: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving message: " + e.getMessage(), e);
        }
    }

    public List<ChatMessage> getChatHistory(String senderUsername, String receiverUsername) {
        logger.debug("📜 Getting chat history between {} and {}", senderUsername, receiverUsername);
        List<ChatMessage> history = chatMessageRepository.findChatHistory(senderUsername, receiverUsername);
        logger.debug("📜 Found {} messages in history", history.size());
        
        // Debug log the first few messages if any exist
        if (!history.isEmpty()) {
            int messagesToLog = Math.min(history.size(), 3);
            for (int i = 0; i < messagesToLog; i++) {
                ChatMessage msg = history.get(i);
                logger.debug("📜 Sample message {}: From {} to {}: {}", 
                    i+1, msg.getSender().getUsername(), msg.getReceiver().getUsername(), msg.getMessageContent());
            }
        }
        
        return history;
    }
    
    /**
     * Retrieves a list of all public chat messages
     * @return List of public messages
     */
    public List<ChatMessage> getAllPublicMessages() {
        return chatMessageRepository.findPublicMessages();
    }
    
    /**
     * Retrieves the 50 most recent public chat messages
     * @return List of recent public messages
     */
    public List<ChatMessage> getRecentPublicMessages() {
        return chatMessageRepository.findRecentPublicMessages();
    }
    
    /**
     * Finds all users that have exchanged messages with the specified user
     * @param username The username to find conversation partners for
     * @return A list of usernames that have had conversations with this user
     */
    public List<String> getUserConversationPartners(String username) {
        Set<String> partners = new HashSet<>();
        
        // Find all messages where user is sender or receiver
        List<ChatMessage> allUserMessages = chatMessageRepository.findAllBySenderUsernameOrReceiverUsername(username, username);
        logger.debug("👥 Found {} total messages for user {}", allUserMessages.size(), username);
        
        // Extract unique users excluding PUBLIC_CHAT and the user themselves
        for (ChatMessage message : allUserMessages) {
            String senderUsername = message.getSender().getUsername();
            String receiverUsername = message.getReceiver().getUsername();
            
            if (!senderUsername.equals(username) && !senderUsername.equals("PUBLIC_CHAT")) {
                partners.add(senderUsername);
                logger.debug("👥 Adding conversation partner {} (as sender)", senderUsername);
            }
            
            if (!receiverUsername.equals(username) && !receiverUsername.equals("PUBLIC_CHAT")) {
                partners.add(receiverUsername);
                logger.debug("👥 Adding conversation partner {} (as receiver)", receiverUsername);
            }
        }
        
        logger.debug("👥 Found {} unique conversation partners for {}", partners.size(), username);
        return new ArrayList<>(partners);
    }
    
    /**
     * Converts chat message entities to DTOs for clients
     * @param messages List of ChatMessage entities
     * @return List of ChatMessageDTO objects
     */
    public List<ChatMessageDTO> convertToDTO(List<ChatMessage> messages) {
        return messages.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert a ChatMessage entity to a ChatMessageDTO
     */
    public ChatMessageDTO entityToDTO(ChatMessage entity) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(entity.getId());
        dto.setSenderUsername(entity.getSender().getUsername());
        dto.setReceiverUsername(entity.getReceiver().getUsername());
        dto.setMessageContent(entity.getMessageContent());
        dto.setTimestamp(entity.getTimestamp().toString());
        
        // Include ticketId and clientMessageId if available
        if (entity.getTicketId() != null) {
            dto.setTicketId(entity.getTicketId());
        }
        
        if (entity.getClientMessageId() != null) {
            dto.setClientMessageId(entity.getClientMessageId());
        }
        
        return dto;
    }
    
    /**
     * Debug method to get all messages sent by a specific user
     */
    public List<ChatMessage> getAllMessagesSentByUser(String username) {
        List<ChatMessage> messages = chatMessageRepository.findAllSentByUsername(username);
        logger.debug("🔍 Found {} messages sent by {}", messages.size(), username);
        return messages;
    }
    
    /**
     * Debug method to get all messages received by a specific user
     */
    public List<ChatMessage> getAllMessagesReceivedByUser(String username) {
        List<ChatMessage> messages = chatMessageRepository.findAllReceivedByUsername(username);
        logger.debug("🔍 Found {} messages received by {}", messages.size(), username);
        return messages;
    }

    /**
     * Get messages related to a specific ticket ID
     */
    public List<ChatMessage> getMessagesByTicketId(String ticketId) {
        try {
            logger.info("🔍 Searching for messages with ticket ID: {}", ticketId);
            
            // Use the repository to find messages where the ticketId matches
            List<ChatMessage> messages = chatMessageRepository.findByTicketId(ticketId);
            
            if (messages != null && !messages.isEmpty()) {
                logger.info("✅ Found {} messages for ticket ID {} via repository query", messages.size(), ticketId);
                return messages;
            }
            
            // If no messages found, try a broader search
            logger.info("⚠️ No messages found with exact ticket ID match, trying content search");
            
            // Get all messages and filter by content mentioning this ticket
            List<ChatMessage> allMessages = chatMessageRepository.findAll();
            
            // Filter messages that mention this ticket ID in the content
            List<ChatMessage> filteredMessages = allMessages.stream()
                .filter(msg -> {
                    String content = msg.getMessageContent().toLowerCase();
                    return content.contains("ticket #" + ticketId) || 
                           content.contains("ticket " + ticketId) ||
                           content.contains("ticket#" + ticketId) ||
                           content.contains("#" + ticketId);
                })
                .collect(Collectors.toList());
            
            logger.info("✅ Found {} messages mentioning ticket ID {} in content", 
                        filteredMessages.size(), ticketId);
                        
            // If we still don't have messages, return an empty list but log this
            if (filteredMessages.isEmpty()) {
                // Create a system message for this ticket to get started
                logger.info("📝 Creating initial system message for empty ticket chat {}", ticketId);
                
                try {
                    User systemUser = userRepository.findByUsername("SYSTEM")
                        .orElseGet(() -> {
                            // Create SYSTEM user if it doesn't exist
                            User system = new User();
                            system.setUsername("SYSTEM");
                            system.setEmail("system@system.com");
                            system.setPassword("$2a$10$dummypasswordhash");
                            return userRepository.save(system);
                        });
                    
                    User publicUser = userRepository.findByUsername("PUBLIC_CHAT")
                        .orElseGet(() -> {
                            // Create PUBLIC_CHAT user if it doesn't exist
                            User publicChat = new User();
                            publicChat.setUsername("PUBLIC_CHAT");
                            publicChat.setEmail("public@system.com");
                            publicChat.setPassword("$2a$10$dummypasswordhash");
                            return userRepository.save(publicChat);
                        });
                    
                    // Create welcome message
                    ChatMessage welcomeMessage = new ChatMessage();
                    welcomeMessage.setSender(systemUser);
                    welcomeMessage.setReceiver(publicUser);
                    welcomeMessage.setMessageContent("Welcome to the support chat for ticket #" + ticketId);
                    welcomeMessage.setTimestamp(LocalDateTime.now());
                    welcomeMessage.setTicketId(ticketId);
                    
                    ChatMessage savedMessage = chatMessageRepository.save(welcomeMessage);
                    logger.info("✅ Created welcome message for ticket {}: {}", ticketId, savedMessage.getId());
                    
                    return Collections.singletonList(savedMessage);
                } catch (Exception e) {
                    logger.error("❌ Error creating welcome message: {}", e.getMessage(), e);
                }
            }
            
            return filteredMessages;
        } catch (Exception e) {
            logger.error("❌ Error retrieving messages for ticket ID {}: {}", ticketId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all messages and participant information for a specific ticket
     * @param ticketId The ticket ID to get messages for
     * @return Map with ticket info, messages, and participants
     */
    public Map<String, Object> getTicketChatDetails(String ticketId) {
        try {
            logger.info("🎫 Getting chat details for ticket ID: {}", ticketId);
            
            // Get all messages for this ticket
            List<ChatMessage> messages = getMessagesByTicketId(ticketId);
            List<ChatMessageDTO> messageDtos = convertToDTO(messages);
            
            logger.info("🎫 Found {} messages for ticket ID {}: {}", 
                messages.size(), ticketId, 
                messages.stream().map(m -> m.getId() + ":" + m.getMessageContent()).collect(Collectors.joining(", ")));
            
            // Extract unique participants
            Set<String> participants = new HashSet<>();
            String clientUsername = null;
            String agentUsername = null;
            
            // Always add essential users (admin and known agents)
            participants.add("admin");
            participants.add("neil");  // Admin user
            participants.add("marie"); // Agent
            
            // Analyze messages to determine participants
            for (ChatMessage message : messages) {
                String sender = message.getSender().getUsername();
                String receiver = message.getReceiver().getUsername();
                
                if (!sender.equals("SYSTEM") && !sender.equals("PUBLIC_CHAT")) {
                    participants.add(sender);
                    logger.info("👤 Added message sender as participant: {}", sender);
                }
                
                if (!receiver.equals("SYSTEM") && !receiver.equals("PUBLIC_CHAT") && !receiver.startsWith("TICKET_")) {
                    participants.add(receiver);
                    logger.info("👤 Added message receiver as participant: {}", receiver);
                }
                
                // Try to determine client and agent (this logic can be improved later)
                // For now, we'll assume the client is the one who sent the first message
                if (clientUsername == null && messages.indexOf(message) == 0) {
                    clientUsername = sender;
                    logger.info("👤 Determined client is likely: {}", clientUsername);
                }
                
                // And the agent is typically another participant who's not the client
                if (agentUsername == null && !sender.equals(clientUsername) && 
                    !sender.equals("SYSTEM") && !sender.equals("PUBLIC_CHAT")) {
                    agentUsername = sender;
                    logger.info("👤 Determined agent is likely: {}", agentUsername);
                }
            }
            
            // Add ticket creator and assigned agent based on ticketId lookup (if implemented)
            // TODO: In a real implementation, you'd look up the ticket by ID and add the client and agent

            // Log all participants 
            logger.info("👥 Final participants for ticket {}: {}", ticketId, participants);
            
            // Create result map
            Map<String, Object> result = new HashMap<>();
            result.put("ticketId", ticketId);
            result.put("messages", messageDtos);
            result.put("participants", new ArrayList<>(participants));
            result.put("messageCount", messages.size());
            
            // Add client and agent info if we determined them
            if (clientUsername != null) {
                result.put("clientUsername", clientUsername);
                logger.info("👤 Set clientUsername in result: {}", clientUsername);
            }
            
            if (agentUsername != null) {
                result.put("agentUsername", agentUsername);
                logger.info("👤 Set agentUsername in result: {}", agentUsername);
            }
            
            logger.info("✅ Chat details for ticket {}: {} messages, {} participants", 
                    ticketId, messages.size(), participants.size());
            
            return result;
        } catch (Exception e) {
            logger.error("❌ Error getting ticket chat details: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("ticketId", ticketId);
            errorResult.put("error", e.getMessage());
            errorResult.put("messages", new ArrayList<>());
            errorResult.put("participants", new ArrayList<>(Arrays.asList("admin", "neil", "marie")));
            return errorResult;
        }
    }

    /**
     * Check if a message with the given clientMessageId already exists
     * @param clientMessageId The client message ID to check
     * @return true if the clientMessageId already exists in the database
     */
    public boolean isClientMessageIdDuplicate(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isEmpty()) {
            return false;
        }
        
        try {
            // Find messages with this clientMessageId
            Optional<ChatMessage> existingMessage = chatMessageRepository.findByClientMessageId(clientMessageId);
            
            if (existingMessage.isPresent()) {
                logger.info("🔍 Found existing message with clientMessageId {}: {}", 
                    clientMessageId, existingMessage.get().getId());
                return true;
            }
            
            logger.info("🔍 No existing message found with clientMessageId: {}", clientMessageId);
            return false;
        } catch (Exception e) {
            logger.error("❌ Error checking for duplicate clientMessageId {}: {}", 
                clientMessageId, e.getMessage(), e);
            // In case of error, assume it's not a duplicate to avoid losing messages
            return false;
        }
    }
}
