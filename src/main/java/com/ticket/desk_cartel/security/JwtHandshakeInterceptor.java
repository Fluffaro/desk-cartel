package com.ticket.desk_cartel.security;

import com.ticket.desk_cartel.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        logger.info("🔍 WebSocket Handshake Started...");

        String query = request.getURI().getQuery();  // e.g., "token=abc123&foo=bar"
        logger.debug("Query String: {}", query);

        if (!StringUtils.hasText(query)) {
            logger.warn("❌ No query parameters found in WebSocket request.");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        // 1. Parse query params (split by '&')
        String[] pairs = query.split("&");
        String tokenValue = null;

        for (String pair : pairs) {
            if (pair.startsWith("token=")) {
                // Extract everything after 'token='
                tokenValue = pair.substring("token=".length());
                break;
            }
        }

        if (!StringUtils.hasText(tokenValue)) {
            logger.warn("❌ No 'token' parameter found in WebSocket request.");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        // 2. URL-decode the token (in case it was URL-encoded)
        try {
            tokenValue = URLDecoder.decode(tokenValue, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.error("❌ Failed to decode token: {}", e.getMessage());
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        logger.info("📌 Extracted Token: {}", tokenValue);

        // 3. Validate token using JwtUtil
        String username = jwtUtil.extractUsername(tokenValue);
        if (username != null && jwtUtil.validateToken(tokenValue, username)) {
            logger.info("✅ Token Valid. User: {}", username);
            attributes.put("username", username);
            return true; // ✅ Allow WebSocket connection
        } else {
            logger.warn("❌ Invalid or expired JWT Token: {}", tokenValue);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No post-handshake logic needed
    }
}
