package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.dto.UserDTO;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing user-related operations, focusing on user management
 * after authentication is complete. Implements UserDetailsService for integration
 * with Spring Security.
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    /**
     * Constructor-based dependency injection.
     *
     * @param userRepository Repository for user data access.
     */
    public UserService(UserRepository userRepository, AgentRepository agentRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Find user by ID.
     *
     * @param id The user ID
     * @return An Optional containing the User if found.
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Find user by username.
     *
     * @param username The username
     * @return An Optional containing the User if found.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Enables or disables a user account.
     *
     * @param userId   The ID of the user.
     * @param isActive true to enable the account, false to disable.
     * @throws RuntimeException if the user is not found.
     */
    public void updateAccountStatus(Long userId, boolean isActive) {
        logger.info("Updating account status for userId: {} to isActive: {}", userId, isActive);
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setActive(isActive);
            userRepository.save(user);
            logger.info("Account status updated for user: {}", user.getUsername());
        } else {
            logger.error("Update failed: User with id {} not found.", userId);
            throw new RuntimeException("User not found");
        }
    }

    /**
     * Loads a user by username for authentication.
     *
     * @param username The username of the user.
     * @return A UserDetails object for Spring Security.
     * @throws UsernameNotFoundException if the user is not found.
     * @throws RuntimeException          if the account is inactive or not verified.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found");
                });

        if (!user.isActive()) {
            logger.error("User account is deactivated: {}", username);
            throw new RuntimeException("User account is deactivated");
        }

        if (!user.isVerified()) {
            logger.error("User email is not verified: {}", username);
            throw new RuntimeException("User email is not verified");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getDateOfBirth(),
                        user.getPhoneNumber(),
                        user.getAddress(),
                        user.getProfilePictureUrl(),
                        user.getRole(),
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Update the role of a user.
     */
    @Transactional
    public void updateUserRole(Long userId, String role) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        if ("AGENT".equalsIgnoreCase(role)) {
            Optional<Agent> existingAgentOpt = agentRepository.findByUser(user);

            if (existingAgentOpt.isPresent()) {
                Agent existingAgent = existingAgentOpt.get();

                if (existingAgent.isActive()) {
                    throw new RuntimeException("User is already an active agent.");
                }

                // Reactivate the agent if they were previously inactive
                existingAgent.setActive(true);
                agentRepository.save(existingAgent);
            } else {
                // Assign as a new agent
                Agent newAgent = new Agent(user, AgentLevel.JUNIOR);
                newAgent.setActive(true);
                agentRepository.save(newAgent);
            }
        } else {
            // If demoting/removing an agent, ensure they have no ongoing tickets
            Optional<Agent> agentOpt = agentRepository.findByUser(user);
            if (agentOpt.isPresent()) {
                Agent agent = agentOpt.get();
                int ongoingTickets = ticketRepository.countOngoingTickets(agent);

                if (ongoingTickets > 0) {
                    throw new RuntimeException("Cannot remove agent role. The agent has ongoing tickets.");
                }

                agent.setActive(false);
                agentRepository.save(agent);
            }
        }

        user.setRole(role);
        userRepository.save(user);
    }

}
