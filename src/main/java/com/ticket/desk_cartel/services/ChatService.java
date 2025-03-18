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

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public ChatMessage saveMessage(ChatMessageDTO chatMessageDTO) {
        User sender = userRepository.findByUsername(chatMessageDTO.getSenderUsername())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByUsername(chatMessageDTO.getReceiverUsername())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setMessageContent(chatMessageDTO.getMessageContent());
        chatMessage.setTimestamp(LocalDateTime.now());
        
        logger.debug("💾 Saving message from {} to {}: {}", 
            sender.getUsername(), receiver.getUsername(), chatMessageDTO.getMessageContent());
        
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        logger.debug("💾 Saved message with ID: {}", savedMessage.getId());
        
        return savedMessage;
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
        if (messages == null) {
            return new ArrayList<>();
        }
        
        return messages.stream()
            .map(msg -> {
                ChatMessageDTO dto = new ChatMessageDTO();
                dto.setSenderUsername(msg.getSender().getUsername());
                dto.setReceiverUsername(msg.getReceiver().getUsername());
                dto.setMessageContent(msg.getMessageContent());
                dto.setTimestamp(msg.getTimestamp());
                return dto;
            })
            .collect(Collectors.toList());
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
}
