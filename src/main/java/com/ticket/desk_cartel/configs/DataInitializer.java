package com.ticket.desk_cartel.configs;

import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.services.PriorityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Component responsible for initializing necessary data when the application starts.
 * This includes creating system users like PUBLIC_CHAT and initializing priority levels.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PriorityService priorityService;
    
    public DataInitializer(UserRepository userRepository, 
                          BCryptPasswordEncoder passwordEncoder,
                          PriorityService priorityService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.priorityService = priorityService;
    }
    
    @Override
    public void run(String... args) {
        createPublicChatUser();
        initializePriorityLevels();
    }
    
    /**
     * Creates a special system user for public chat messages if it doesn't exist.
     */
    private void createPublicChatUser() {
        if (!userRepository.existsByUsername("PUBLIC_CHAT")) {
            logger.info("Creating PUBLIC_CHAT system user for public messages");
            
            User publicChatUser = new User();
            publicChatUser.setUsername("PUBLIC_CHAT");
            publicChatUser.setEmail("public-chat@system.local");
            publicChatUser.setFullName("Public Chat System");
            // Generate a random password as this user will never login
            publicChatUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            publicChatUser.setRole("SYSTEM");
            publicChatUser.setActive(true);
            publicChatUser.setVerified(true);
            
            userRepository.save(publicChatUser);
            logger.info("PUBLIC_CHAT system user created successfully");
        } else {
            logger.info("PUBLIC_CHAT system user already exists");
        }
    }
    
    /**
     * Initializes default priority levels if they don't exist in the database.
     */
    private void initializePriorityLevels() {
        try {
            logger.info("Initializing default priority levels");
            priorityService.initializeDefaultPriorities();
            logger.info("Priority levels initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize priority levels", e);
        }
    }
} 