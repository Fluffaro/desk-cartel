package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.dto.ChatMessageDTO;
import com.ticket.desk_cartel.entities.ChatMessage;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.ChatMessageRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

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

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(String senderUsername, String receiverUsername) {
        return chatMessageRepository.findChatHistory(senderUsername, receiverUsername);
    }
}
