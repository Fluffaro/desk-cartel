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
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("http://127.0.0.1:5500", "http://localhost:3000", "http://localhost:8080")  // ✅ Allow WebSocket connections from Live Server
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil));


        registry.addEndpoint("/ws-chat-sockjs")
                .setAllowedOrigins("http://127.0.0.1:5500", "http://localhost:3000", "http://localhost:8080")  // ✅ Allow WebSocket connections from Live Server
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .withSockJS()  // ✅ Enable SockJS fallback
                .setHeartbeatTime(25000); // Set SockJS heartbeat to 25 seconds (default is 25s)
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
        // This MUST match the prefix used in client subscriptions
        registry.setUserDestinationPrefix("/user");
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
                    // Get username from session attributes
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
                        String username = (String) sessionAttributes.get("username");
                        logger.info("✅ Setting username in CONNECT message: {}", username);
                        accessor.setUser(new Principal() {
                            @Override
                            public String getName() {
                                return username;
                            }
                        });
                    }
                }
                return message;
            }
        });
    }
}
