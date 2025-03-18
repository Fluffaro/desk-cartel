package com.ticket.desk_cartel.configs;

import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Component responsible for initializing necessary data when the application starts.
 * This includes creating system users like PUBLIC_CHAT.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public DataInitializer(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) {
        createPublicChatUser();
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
} 