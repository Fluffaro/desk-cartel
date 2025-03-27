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
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private final JwtUtil jwtUtil;
    private static final List<String> SOCKJS_PATHS = Arrays.asList(
        "/websocket", "/xhr", "/xhr_send", "/xhr_streaming", "/eventsource", "/htmlfile"
    );

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        URI uri = request.getURI();
        String path = uri.getPath();
        logger.info("🔍 WebSocket Handshake Started... URI: {}", uri);
        
        // Check if this is a SockJS transport endpoint
        boolean isSockJSTransport = false;
        for (String sockJSPath : SOCKJS_PATHS) {
            if (path.endsWith(sockJSPath)) {
                isSockJSTransport = true;
                logger.info("📱 Detected SockJS transport endpoint: {}", sockJSPath);
                break;
            }
        }
        
        // If this is a SockJS transport endpoint or info endpoint, we may already have the token in session attributes
        // This will be set by the first SockJS handshake and should be reused for subsequent connections
        if (isSockJSTransport || path.contains("/info")) {
            // Check if we already have username in session attributes (set from previous SockJS handshake)
            if (attributes.containsKey("username")) {
                logger.info("✅ Reusing existing username from session attributes: {}", attributes.get("username"));
                return true;
            }
        }
        
        // First try to get the token from query parameters
        String tokenValue = extractTokenFromQuery(request);
        
        // If not found in query, check for Authorization header
        if (tokenValue == null) {
            tokenValue = extractTokenFromHeader(request);
        }
        
        // If still not found, reject the connection
        if (tokenValue == null) {
            logger.warn("❌ No token found in request (neither in query params nor headers)");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        
        // Log token for debugging (partially masked)
        if (tokenValue.length() > 20) {
            logger.info("📌 Extracted Token: {}...{}", 
                tokenValue.substring(0, 10), 
                tokenValue.substring(tokenValue.length() - 10));
        } else {
            logger.info("📌 Extracted Token: {}", tokenValue);
        }

        // Validate token using JwtUtil
        try {
            String username = jwtUtil.extractUsername(tokenValue);
            if (username != null && jwtUtil.validateToken(tokenValue, username)) {
                logger.info("✅ Token Valid. User: {}", username);
                
                // Store both username and token in attributes for future use
                attributes.put("username", username);
                attributes.put("token", tokenValue);
                
                // Debug info to check attributes are properly set
                logger.info("✅ Set username in session attributes: {}", username);
                
                return true; // ✅ Allow WebSocket connection
            } else {
                logger.warn("❌ Invalid or expired JWT Token. Username extracted: {}", username);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Exception during token validation: {}", e.getMessage(), e);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    /**
     * Extract token from query parameters
     */
    private String extractTokenFromQuery(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        
        if (!StringUtils.hasText(query)) {
            return null;
        }
        
        logger.debug("Query String: {}", query);
        
        // Parse query params (split by '&')
        String[] pairs = query.split("&");
        
        for (String pair : pairs) {
            if (pair.startsWith("token=")) {
                // Extract everything after 'token='
                String tokenValue = pair.substring("token=".length());
                
                // URL-decode the token (in case it was URL-encoded)
                try {
                    tokenValue = URLDecoder.decode(tokenValue, StandardCharsets.UTF_8.name());
                    logger.debug("Found token in query params: {}...", 
                        tokenValue.length() > 10 ? tokenValue.substring(0, 10) + "..." : tokenValue);
                    return tokenValue;
                } catch (UnsupportedEncodingException e) {
                    logger.error("❌ Failed to decode token: {}", e.getMessage());
                    return null;
                }
            }
        }
        
        logger.debug("No token found in query parameters");
        return null;
    }

    /**
     * Extract token from Authorization header
     */
    private String extractTokenFromHeader(ServerHttpRequest request) {
        // Try to get token from Authorization header
        String authHeader = null;
        if (request.getHeaders().containsKey("Authorization")) {
            authHeader = request.getHeaders().getFirst("Authorization");
        }
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            logger.debug("Found Authorization header with Bearer token");
            return authHeader.substring(7); // Remove "Bearer " prefix
        }
        
        logger.debug("No token found in Authorization header");
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            logger.error("❌ Exception after handshake: {}", exception.getMessage(), exception);
        } else {
            logger.debug("✅ Handshake completed successfully for {}", request.getURI());
        }
    }
}
