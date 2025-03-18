package com.ticket.desk_cartel.configs;

import com.ticket.desk_cartel.security.JwtUtil;
import com.ticket.desk_cartel.security.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("*")  // ✅ Allow WebSocket connections from any origin
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil));


        registry.addEndpoint("/ws-chat-sockjs")
                .setAllowedOrigins("*")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .withSockJS();// ✅ Validate JWT before connection
                //.withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
