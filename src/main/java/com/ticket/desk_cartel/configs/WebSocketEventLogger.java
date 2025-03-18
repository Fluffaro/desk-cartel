package com.ticket.desk_cartel.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;

import java.lang.reflect.Type;

public class WebSocketEventLogger extends StompSessionHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventLogger.class);

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        logger.info("Connected to WebSocket: " + session.getSessionId());
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        logger.error("WebSocket error: " + exception.getMessage());
    }
}
