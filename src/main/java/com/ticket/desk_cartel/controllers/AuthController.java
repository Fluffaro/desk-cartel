package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.dto.LoginRequestDTO;
import com.ticket.desk_cartel.dto.RegisterRequestDTO;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling authentication operations such as login, registration, and verification.
 * Follows the Single Responsibility Principle by focusing only on authentication concerns.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param registerRequest DTO containing registration details.
     * @return ResponseEntity indicating success or error details.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            // Basic input validation
            if (registerRequest.getUsername() == null || registerRequest.getEmail() == null ||
                    registerRequest.getPassword() == null || registerRequest.getFullName() == null) {
                logger.warn("Registration failed: Missing required fields.");
                return ResponseEntity.badRequest().body("Missing required fields: username, email, fullName, or password.");
            }
            
            // Register the user using the service
            User registeredUser = authService.register(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getFullName(),
                    registerRequest.getPassword(),
                    registerRequest.getDateOfBirth(),
                    registerRequest.getPhoneNumber(),
                    registerRequest.getAddress()
            );
            
            logger.info("User registered successfully: {}", registeredUser.getUsername());
            return ResponseEntity.ok(Map.of("message", "User registered successfully. Please verify your email."));
        } catch (RuntimeException e) {
            logger.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Verifies a user's account using a provided token.
     *
     * @param token Verification token.
     * @return ResponseEntity indicating the result of verification.
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam String token) {
        Optional<User> userOptional = authService.verifyToken(token);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setVerified(true);
            userRepository.save(user);
            logger.info("User verified successfully: {}", user.getUsername());
            return ResponseEntity.ok("✅ Account verified successfully. You can now log in.");
        } else {
            logger.warn("Verification failed for token: {}", token);
            return ResponseEntity.badRequest().body("❌ Invalid or expired token.");
        }
    }

    /**
     * Authenticates a user and generates a JWT token upon successful login.
     *
     * @param loginRequest DTO containing login credentials.
     * @return ResponseEntity with JWT token or error message.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401).body("Invalid Credentials");
        }
    }
} 