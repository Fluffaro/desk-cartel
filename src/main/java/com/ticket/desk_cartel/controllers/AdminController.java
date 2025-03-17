package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;

    public AdminController(UserRepository userRepository, AgentRepository agentRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
    }

    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user role
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setRole(role);
        userRepository.save(user);

        if ("AGENT".equalsIgnoreCase(role)) {
            agentRepository.findByUser(user).orElseGet(() -> {
                Agent newAgent = new Agent(user, AgentLevel.JUNIOR);
                return agentRepository.save(newAgent);
            });
        }

        return ResponseEntity.ok("User role updated successfully.");
    }
}
