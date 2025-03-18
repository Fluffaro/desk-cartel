package com.ticket.desk_cartel.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    // Map to track active WebSocket sessions by username
    private static final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        if (event == null) {
            logger.warn("⚠️ Received null SessionConnectedEvent");
            return;
        }
        
        logger.info("🌐 WebSocket SessionConnectedEvent received: {}", event);
        
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor == null) {
            logger.warn("⚠️ Could not extract StompHeaderAccessor from event");
            return;
        }
        
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            logger.warn("⚠️ Session ID is null");
            return;
        }
        
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        logger.info("🌐 Session attributes: {}", sessionAttributes);
        logger.info("🌐 Message headers: {}", headerAccessor.getMessageHeaders());
        
        // Check if Principal is set
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            if (username != null && !username.isEmpty()) {
                logger.info("🌐 Principal found in header: {}", username);
                userSessionMap.put(username, sessionId);
                logger.info("🌐 User connected: {} with session ID: {}", username, sessionId);
                logger.info("🌐 Active WebSocket sessions: {}", userSessionMap);
                
                // Send a test message to the user
                try {
                    logger.info("🌐 Attempting to send test message to user: {}", username);
                    messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/private-messages",
                        Map.of(
                            "senderUsername", "SYSTEM",
                            "messageContent", "Connected successfully! Your private messages will appear here."
                        )
                    );
                    logger.info("🌐 Test message sent successfully");
                } catch (Exception e) {
                    logger.error("🌐 Error sending test message", e);
                }
            } else {
                logger.warn("⚠️ Principal username is null or empty");
            }
        }

        // Only try to get username from session attributes if they exist
        if (sessionAttributes != null) {
            Object rawUsername = sessionAttributes.get("username");
            
            if (rawUsername != null) {
                String username = (String) rawUsername;
                userSessionMap.put(username, sessionId);
                logger.info("🌐 User connected from session attributes: {} with session ID: {}", username, sessionId);
                logger.info("🌐 Active WebSocket sessions: {}", userSessionMap);
            } else {
                logger.warn("⚠️ Connected session without username: {}", sessionId);
                logger.warn("⚠️ Session attributes may not be properly set during handshake");
            }
        } else {
            logger.warn("⚠️ Session attributes are null for session: {}", sessionId);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        if (event == null) {
            logger.warn("⚠️ Received null SessionDisconnectEvent");
            return;
        }
        
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor == null) {
            logger.warn("⚠️ Could not extract StompHeaderAccessor from disconnect event");
            return;
        }
        
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            logger.warn("⚠️ Disconnect session ID is null");
            return;
        }
        
        // Find and remove the username associated with this session
        userSessionMap.entrySet().removeIf(entry -> sessionId.equals(entry.getValue()));
        
        logger.info("🌐 Session disconnected: {}", sessionId);
        logger.info("🌐 Remaining active WebSocket sessions: {}", userSessionMap);
    }
    
    // Utility method to get session ID by username
    public static String getSessionId(String username) {
        return username != null ? userSessionMap.get(username) : null;
    }
    
    // Utility method to check if a user is connected
    public static boolean isUserConnected(String username) {
        return username != null && userSessionMap.containsKey(username);
    }
    
    // Utility method to get all active session information for debugging
    public static Map<String, Object> getActiveSessionsInfo() {
        Map<String, Object> sessionsInfo = new HashMap<>();
        sessionsInfo.put("sessionCount", userSessionMap.size());
        sessionsInfo.put("activeSessions", new HashMap<>(userSessionMap)); // Create copy to avoid modification issues
        return sessionsInfo;
    }
} 