package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Controller for managing user-related operations.
 * <p>
 * This controller handles endpoints for user profile management and information retrieval.
 * Authentication-related concerns have been moved to AuthController.
 * </p>
 */
@RestController
@RequestMapping("${api.user.base-url}")
public class UserController {

    // Logger for logging important events and errors.
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieve the currently authenticated user's information.
     *
     * @param principal Security principal containing the username.
     * @return ResponseEntity with user details or an error message.
     */
    @GetMapping("${api.user.me}")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            logger.warn("Unauthorized access attempt to /me endpoint.");
            // Return a JSON response with an error message
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Optional<User> userOptional = userRepository.findByUsername(principal.getName());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("fullName", user.getFullName());
            userInfo.put("Id", user.getId());
            userInfo.put("role", user.getRole());
            userInfo.put("email", user.getEmail());
            logger.info("Retrieved user info for user: {}", user.getUsername());
            return ResponseEntity.ok(userInfo);
        } else {
            logger.error("User not found for username: {}", principal.getName());
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
    }
}
