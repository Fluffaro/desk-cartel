package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * Service handling authentication-related operations such as login, registration, and verification.
 * Extracts these responsibilities from UserService to follow the Single Responsibility Principle.
 */
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final VerificationTokenService verificationTokenService;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       VerificationTokenService verificationTokenService,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenService = verificationTokenService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user with the provided credentials.
     *
     * @param username The username.
     * @param rawPassword The raw password input.
     * @return A JWT token if authentication is successful.
     * @throws RuntimeException if authentication fails.
     */
    public String login(String username, String rawPassword) {
        Optional<User> foundUser = findActiveVerifiedUser(username);
        if (foundUser.isEmpty()) {
            logger.warn("Login failed: User not found or inactive/unverified for username: {}", username);
            throw new RuntimeException("Invalid Credentials");
        }
        User user = foundUser.get();

        if (!passwordMatches(rawPassword, user.getPassword())) {
            logger.warn("Login failed: Incorrect password for username: {}", username);
            throw new RuntimeException("Invalid Credentials");
        }

        // Generate and return JWT token.
        String token = jwtUtil.generateToken(username, user.getRole());
        logger.info("User logged in successfully: {}", username);
        return token;
    }

    /**
     * Registers a new user with provided details and sends verification email.
     *
     * @param username User's username.
     * @param email User's email address.
     * @param fullName User's full name.
     * @param password User's raw password.
     * @param dateOfBirth User's date of birth.
     * @param phoneNumber User's phone number.
     * @param address User's address.
     * @return The newly registered User.
     * @throws RuntimeException if the username or email already exists.
     */
    public User register(String username, String email, String fullName, String password,
                         Date dateOfBirth, String phoneNumber, String address) {
        logger.info("Attempting to register user with username: {}", username);

        if (userRepository.existsByUsername(username)) {
            logger.error("Registration failed: Username {} already exists.", username);
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            logger.error("Registration failed: Email {} already exists.", email);
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(password));
        user.setDateOfBirth(dateOfBirth);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
        user.setRole("USER");
        user.setActive(true);
        user.setVerified(false);

        userRepository.save(user);
        logger.info("User registered successfully: {}", username);

        verificationTokenService.sendVerificationEmail(user);
        logger.info("Verification email sent to: {}", email);

        return user;
    }

    /**
     * Verifies a user account using a token.
     *
     * @param token The verification token.
     * @return Optional containing the verified User if successful, empty Optional otherwise.
     */
    public Optional<User> verifyToken(String token) {
        return verificationTokenService.verifyToken(token);
    }

    /**
     * Finds an active and verified user by username.
     *
     * @param username The username to search for.
     * @return An Optional containing the User if found and verified.
     */
    public Optional<User> findActiveVerifiedUser(String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username)
                .filter(User::isVerified);
    }

    /**
     * Checks if the provided raw password matches the stored encoded password.
     *
     * @param rawPassword The raw password.
     * @param encodedPassword The stored encoded password.
     * @return true if passwords match, false otherwise.
     */
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
} 