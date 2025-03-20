package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiAIService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    // Google Gemini API endpoint
    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    
    /**
     * Suggests a priority for a ticket using Google Gemini AI with fallback to keyword matching
     * 
     * @param title Ticket title
     * @param description Ticket description
     * @param category Ticket category name
     * @return Suggested Priority
     */
    public Priority suggestPriority(String title, String description, String category) {
        logger.info("Suggesting priority for ticket - Title: \"{}\", Category: \"{}\", Description length: {} chars", 
                title, category, description != null ? description.length() : 0);
                
        try {
            logger.info("Attempting to use Google Gemini AI for priority suggestion");
            // Try Gemini AI first
            Priority priority = callGeminiForPriority(title, description, category);
            logger.info("Successfully determined priority via Google Gemini AI: {}", priority);
            return priority;
        } catch (Exception e) {
            logger.warn("Google Gemini API failed: {} - {}", e.getClass().getName(), e.getMessage());
            // Fallback to simple keyword matching
            Priority priority = keywordBasedPriority(title, description, category);
            logger.info("Determined priority via keyword matching fallback: {}", priority);
            return priority;
        }
    }
    
    /**
     * Attempts to determine priority using the Google Gemini API
     */
    private Priority callGeminiForPriority(String title, String description, String category) throws IOException {
        // Skip API call if no token is configured
        if (apiKey == null || apiKey.isEmpty()) {
            logger.info("No Google Gemini API key configured, using fallback");
            throw new IOException("No API key configured");
        }
        
        // Improved input with better structure and priority level definitions
        String prompt = String.format(
            "I need to classify the priority of this support ticket:\n\n" +
            "Category: %s\n" +
            "Title: %s\n" +
            "Description: %s\n\n" +
            "NOTE: The priority should be based primarily on the issue's impact, not just the category.\n\n" +
            "Priority levels are defined as follows:\n" +
            "- CRITICAL: Severe system-wide outages, data loss, security breaches, or production-blocking issues affecting many users. Examples: server down, database corruption, ransomware attack, critical service unavailable for multiple teams, widespread virus infection, data breach affecting multiple systems.\n" +
            "- HIGH: Significant functionality broken with no workaround, affecting core business processes or important user data. Examples: application crashes, inability to submit forms, broken checkout process, network outage in one department, user data theft or loss, single user virus/malware that compromised data, inability to access critical files/systems, security issues affecting a single user's data.\n" +
            "- MEDIUM: Important issues with available workarounds, or issues affecting non-critical functions. Examples: slow system performance, minor calculation errors, formatting problems in reports, hardware issues where basic functionality remains (e.g., printer works but double-sided printing fails), suspicious activity without confirmed data loss, non-critical data corruption, virus/malware that has been contained without major data loss.\n" +
            "- LOW: Minor issues, cosmetic problems, \"how-to\" questions, feature requests, or basic usage assistance. Examples: UI alignment issues, spelling errors, password resets, basic usage questions like \"how do I save a file\", training requests, \"I don't know how to use this feature\", advice on avoiding viruses, questions about security best practices without an actual incident.\n\n" +
            "Based on the information provided, what is the priority level for this ticket? Reply with only one word: LOW, MEDIUM, HIGH, or CRITICAL.",
            category, title, description
        );
        
        // Create Gemini API request body
        Map<String, Object> requestMap = new HashMap<>();
        
        // Build the contents array with parts as shown in the curl example
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        
        List<Object> parts = new ArrayList<>();
        parts.add(part);
        content.put("parts", parts);
        
        List<Object> contents = new ArrayList<>();
        contents.add(content);
        requestMap.put("contents", contents);
        
        String requestBody = objectMapper.writeValueAsString(requestMap);
        logger.info("Sending request to Google Gemini API with body: {}", requestBody);
        
        // Build the HTTP request with API key as query parameter
        HttpUrl url = HttpUrl.parse(GEMINI_API_URL)
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .build();
            
        RequestBody body = RequestBody.create(
            requestBody,
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .build();
        
        logger.info("Calling Google Gemini API");
        
        // Execute the request
        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            String responseBody = response.body().string();
            
            logger.info("Google Gemini API response status: {}", statusCode);
            logger.info("Google Gemini API response body: {}", responseBody);
            
            if (!response.isSuccessful()) {
                logger.error("Google Gemini API error - Status: {}, Body: {}", statusCode, responseBody);
                throw new IOException("Unexpected code " + response + ", body: " + responseBody);
            }
            
            // Parse the result to find the priority
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            try {
                // Extract the response text
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode contentNode = candidates.get(0).path("content");
                    JsonNode responseParts = contentNode.path("parts");
                    if (responseParts.isArray() && responseParts.size() > 0) {
                        String text = responseParts.get(0).path("text").asText().trim();
                        logger.info("Extracted priority from Gemini API: {}", text);
                        
                        // Look for priority in the response text
                        for (Priority p : Priority.values()) {
                            if (text.toUpperCase().contains(p.name())) {
                                logger.info("Found priority {} in response", p.name());
                                return p;
                            }
                        }
                        
                        // Try direct mapping if exact priority name found
                        try {
                            return Priority.valueOf(text.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            logger.warn("Could not parse priority from response: {}", text);
                        }
                    }
                }
                
                logger.warn("No clear priority found in response, defaulting to MEDIUM");
                return Priority.MEDIUM;
                
            } catch (Exception e) {
                logger.error("Error parsing API response: {}", e.getMessage());
                logger.error("Response was: {}", responseBody);
                throw new IOException("Error parsing API response", e);
            }
        } catch (Exception e) {
            logger.error("Exception calling Google Gemini API: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Fallback method that determines priority based on keywords in title and description
     */
    private Priority keywordBasedPriority(String title, String description, String category) {
        logger.info("Using keyword-based priority detection as fallback");
        String text = (title + " " + description).toLowerCase();
        
        logger.info("Analyzing text for keyword matching: {}", text);
        
        // Security and data theft issues should generally be HIGH priority
        boolean hasSecurityTerm = text.contains("virus") || text.contains("malware") || 
                                  text.contains("hack") || text.contains("compromise") || 
                                  text.contains("breach") || text.contains("stole") ||
                                  text.contains("theft") || text.contains("data") || 
                                  text.contains("information");
        
        boolean hasDataLossTerm = text.contains("took") || text.contains("stole") || 
                                  text.contains("lost") || text.contains("compromised") || 
                                  text.contains("theft") || text.contains("my information") ||
                                  text.contains("my data") || text.contains("personal information");
        
        // Log what we're detecting
        logger.info("Security term detected: {}, Data loss term detected: {}", hasSecurityTerm, hasDataLossTerm);
        
        if (hasSecurityTerm && hasDataLossTerm) {
            logger.info("Detected HIGH priority based on data theft or security compromise keywords");
            return Priority.HIGH;
        }
        
        // Even just having a virus without data loss is at least HIGH
        if (text.contains("virus") || text.contains("malware") || text.contains("hack") || 
            text.contains("breach") || text.contains("compromise") || text.contains("attack")) {
            logger.info("Detected HIGH priority based on security issue");
            return Priority.HIGH;
        }
        
        // Database failures should be HIGH or CRITICAL
        if (category != null && category.toLowerCase().contains("database")) {
            if (text.contains("broke") || text.contains("broken") || text.contains("not working") ||
                text.contains("down") || text.contains("fail") || text.contains("failed") ||
                text.contains("corrupt") || text.contains("not retrieving") || 
                text.contains("cannot access") || text.contains("unusable")) {
                
                // Multi-user impact suggests CRITICAL
                if (text.contains("all") || text.contains("everyone") || 
                    text.contains("users") || text.contains("teams") || 
                    text.contains("company") || text.contains("organization") ||
                    text.contains("system")) {
                    logger.info("Detected CRITICAL priority for database failure affecting multiple users");
                    return Priority.CRITICAL;
                }
                
                logger.info("Detected HIGH priority for database failure");
                return Priority.HIGH;
            }
        }
        
        // "How-to" and basic usage questions should be LOW priority
        if (text.contains("how to") || text.contains("how do i") || 
            text.contains("don't know how") || text.contains("dont know how") ||
            text.contains("help me with") || text.contains("tutorial") ||
            text.contains("guide me") || text.contains("instructions") ||
            text.contains("password reset") || text.contains("forgot password")) {
            logger.info("Detected LOW priority based on 'how-to' or basic usage keywords");
            return Priority.LOW;
        }
        
        // Critical keywords
        if (text.contains("urgent") || text.contains("emergency") || 
            text.contains("critical") || text.contains("severe") ||
            text.contains("all users affected") || text.contains("everyone affected") ||
            text.contains("breach") || text.contains("attack") ||
            text.contains("data loss") || text.contains("servers down") ||
            text.contains("production down") || text.contains("database corrupt") ||
            text.contains("system offline") || 
            (text.contains("virus") && text.contains("spreading")) ||
            (text.contains("malware") && text.contains("multiple"))) {
            logger.info("Detected CRITICAL priority based on keywords");
            return Priority.CRITICAL;
        }
        
        // High priority keywords
        if (text.contains("important") || text.contains("high priority") ||
            text.contains("significant") || text.contains("affecting multiple users") ||
            text.contains("no workaround") || text.contains("business impact") ||
            text.contains("financial impact") || text.contains("deadline") ||
            text.contains("cannot access") || text.contains("unable to login") ||
            text.contains("application crash") || text.contains("data incorrect") ||
            text.contains("virus") || text.contains("malware") ||
            text.contains("hacked") || text.contains("compromised")) {
            logger.info("Detected HIGH priority based on keywords");
            return Priority.HIGH;
        }
        
        // Low priority keywords
        if (text.contains("minor") || text.contains("low priority") ||
            text.contains("when possible") || text.contains("not urgent") ||
            text.contains("suggestion") || text.contains("enhancement") ||
            text.contains("cosmetic") || text.contains("typo") ||
            text.contains("feature request") || text.contains("would be nice") ||
            text.contains("visual issue") || text.contains("formatting")) {
            logger.info("Detected LOW priority based on keywords");
            return Priority.LOW;
        }
        
        // Hardware-specific logic - most basic hardware issues are MEDIUM unless specified otherwise
        if (category != null && category.toLowerCase().contains("hardware")) {
            // For hardware, check if it's a complete failure or just a basic question
            if (text.contains("completely dead") || text.contains("won't power on") ||
                text.contains("blue screen") || text.contains("not booting") ||
                text.contains("hardware failure")) {
                logger.info("Detected HIGH priority for critical hardware failure");
                return Priority.HIGH;
            } else if (text.contains("how to") || text.contains("help with") || 
                       text.contains("don't know") || text.contains("instructions")) {
                logger.info("Detected LOW priority for hardware usage question");
                return Priority.LOW;
            } else {
                logger.info("Detected MEDIUM priority for general hardware issue");
                return Priority.MEDIUM;
            }
        }
        
        // Consider category-based priority adjustment
        if (category != null) {
            String lowerCategory = category.toLowerCase();
            if (lowerCategory.contains("security") || lowerCategory.contains("critical") ||
                lowerCategory.contains("production")) {
                logger.info("Bumping priority to HIGH based on critical category: {}", category);
                return Priority.HIGH;
            }
        }
        
        // Default to MEDIUM
        logger.info("No specific keyword patterns detected, defaulting to MEDIUM priority");
        return Priority.MEDIUM;
    }
} 