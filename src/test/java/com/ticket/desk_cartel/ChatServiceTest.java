package com.ticket.desk_cartel;

import com.ticket.desk_cartel.dto.ChatMessageDTO;
import com.ticket.desk_cartel.entities.ChatMessage;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.ChatMessageRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User sender, receiver;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setUsername("Alice");

        receiver = new User();
        receiver.setUsername("Bob");

        message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMessageContent("Hello Bob!");
        message.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testSaveMessage_Success() {
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO();
        chatMessageDTO.setSenderUsername("Alice");
        chatMessageDTO.setReceiverUsername("Bob");
        chatMessageDTO.setMessageContent("Hello Bob!");

        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("Bob")).thenReturn(Optional.of(receiver));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);

        ChatMessage savedMessage = chatService.saveMessage(chatMessageDTO);

        assertNotNull(savedMessage);
        assertEquals("Hello Bob!", savedMessage.getMessageContent());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void testGetChatHistory() {
        when(chatMessageRepository.findChatHistory("Alice", "Bob")).thenReturn(List.of(message));

        List<ChatMessage> history = chatService.getChatHistory("Alice", "Bob");

        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
    }
}
