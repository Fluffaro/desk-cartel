package com.ticket.desk_cartel.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class WebSocketExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketExceptionHandler.class);

    /**
     * Handles exceptions in WebSocket message methods and sends an error response to the user.
     * 
     * @param exception The exception that was thrown
     * @return A Map containing error details
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, Object> handleException(Exception exception) {
        logger.error("WebSocket error occurred: {}", exception.getMessage(), exception);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", exception.getMessage());
        
        return errorResponse;
    }
}
