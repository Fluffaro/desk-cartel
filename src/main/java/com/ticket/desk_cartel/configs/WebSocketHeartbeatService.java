package com.ticket.desk_cartel.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket heartbeat service that sends periodic connection status messages
 * to all connected users to keep their connections alive.
 */
@Component
@EnableScheduling
public class WebSocketHeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHeartbeatService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send a heartbeat to all connected users every 50 seconds
     * This helps keep connections alive, especially with proxies that might
     * close idle connections.
     */
    @Scheduled(fixedRate = 50000)
    public void sendHeartbeatToAll() {
        // Get all currently connected users
        Map<String, Object> sessionInfo = WebSocketEventListener.getActiveSessionsInfo();
        Map<String, String> activeSessions = (Map<String, String>) sessionInfo.get("activeSessions");
        
        if (activeSessions == null || activeSessions.isEmpty()) {
            logger.debug("No active sessions for heartbeat");
            return;
        }
        
        logger.debug("Sending heartbeat to {} active users", activeSessions.size());
        
        // Send a heartbeat message to each connected user
        for (String username : activeSessions.keySet()) {
            try {
                // Create a lightweight heartbeat message
                Map<String, Object> heartbeat = new HashMap<>();
                heartbeat.put("type", "heartbeat");
                heartbeat.put("timestamp", System.currentTimeMillis());
                
                // Send to user's heartbeat channel
                messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/heartbeat",
                    heartbeat
                );
            } catch (Exception e) {
                logger.warn("Failed to send heartbeat to user {}: {}", username, e.getMessage());
            }
        }
    }
    
    /**
     * Periodically log connection statistics
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logConnectionStatistics() {
        Map<String, Object> sessionInfo = WebSocketEventListener.getActiveSessionsInfo();
        int sessionCount = (int) sessionInfo.get("sessionCount");
        
        logger.info("WebSocket connection statistics: {} active sessions", sessionCount);
    }
} 