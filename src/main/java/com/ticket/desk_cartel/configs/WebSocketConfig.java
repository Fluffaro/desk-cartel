package com.ticket.desk_cartel.configs;

import com.ticket.desk_cartel.security.JwtUtil;
import com.ticket.desk_cartel.security.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Create array of allowed origins
        String[] allowedOrigins = {
            "http://127.0.0.1:5500", 
            "http://localhost:5500", 
            "http://localhost:8080",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        };
        
        logger.info("📌 Registering WebSocket endpoints...");
        
        // Register regular WebSocket endpoint (for direct WebSocket connections)
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .setHandshakeHandler(new CustomHandshakeHandler());

        // Register SockJS-enabled endpoint (for fallback support)
        registry.addEndpoint("/ws-chat-sockjs")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(
                    // Add session attributes interceptor first to make sure session attributes are copied
                    new HttpSessionHandshakeInterceptor(),
                    // Then our custom JWT interceptor 
                    new JwtHandshakeInterceptor(jwtUtil)
                )
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(30000)
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
        
        logger.info("✅ WebSocket endpoints registered successfully");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for topic and queue destinations with heartbeat
        registry.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[] {10000, 10000}) // Set broker heartbeat - server sends every 10s, expects from client every 10s
            .setTaskScheduler(taskScheduler());
        
        // Set prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");
        
        logger.info("✅ WebSocket message broker configured");
    }

    /**
     * Configure a task scheduler for broker heartbeats
     */
    private org.springframework.scheduling.TaskScheduler taskScheduler() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler = 
            new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-thread-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Log headers for debugging
                    logger.info("🔍 STOMP CONNECT headers: {}", accessor.getMessageHeaders());
                    
                    // Try to get username from different sources
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        // First try from session attributes (set by JwtHandshakeInterceptor)
                        if (sessionAttributes.containsKey("username")) {
                            String username = (String) sessionAttributes.get("username");
                            logger.info("✅ Found username in session attributes: {}", username);
                            
                            accessor.setUser(new Principal() {
                                @Override
                                public String getName() {
                                    return username;
                                }
                            });
                            return message;
                        }
                        
                        // Log for debugging
                        logger.info("Session attributes available: {}", sessionAttributes.keySet());
                    }
                    
                    // Try to get username from connect headers
                    String username = accessor.getFirstNativeHeader("username");
                    if (username != null && !username.isEmpty()) {
                        logger.info("✅ Found username in connect headers: {}", username);
                        
                        // Store in session attributes for future reference
                        if (sessionAttributes != null) {
                            sessionAttributes.put("username", username);
                        }
                        
                        accessor.setUser(new Principal() {
                            @Override
                            public String getName() {
                                return username;
                            }
                        });
                    } else {
                        logger.warn("⚠️ No username found in STOMP CONNECT headers");
                    }
                }
                return message;
            }
        });
    }
}

/**
 * Custom handshake handler to provide better debugging
 */
class CustomHandshakeHandler implements org.springframework.web.socket.server.HandshakeHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomHandshakeHandler.class);
    private final org.springframework.web.socket.server.support.DefaultHandshakeHandler defaultHandler = 
        new org.springframework.web.socket.server.support.DefaultHandshakeHandler();
        
    @Override
    public boolean doHandshake(org.springframework.http.server.ServerHttpRequest request, 
                              org.springframework.http.server.ServerHttpResponse response,
                              org.springframework.web.socket.WebSocketHandler wsHandler,
                              Map<String, Object> attributes) throws org.springframework.web.socket.server.HandshakeFailureException {
        logger.info("👋 Starting WebSocket handshake from {}", request.getRemoteAddress());
        try {
            boolean result = defaultHandler.doHandshake(request, response, wsHandler, attributes);
            if (result) {
                logger.info("✅ Handshake successful for {}", request.getRemoteAddress());
            } else {
                logger.warn("❌ Handshake rejected for {}", request.getRemoteAddress());
            }
            return result;
        } catch (Exception e) {
            logger.error("❌ Handshake error: {}", e.getMessage(), e);
            throw e;
        }
    }
}
