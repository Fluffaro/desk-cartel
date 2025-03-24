package com.ticket.desk_cartel.controllers;


import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ticket.desk_cartel.dto.UserDTO;
import com.ticket.desk_cartel.services.UserService;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("${api.admin.base-url}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {this.userService = userService;}

    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user role.
     */
    @PutMapping("${api.admin.updateRole}")
    public ResponseEntity<String> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateUserRole(id, role);
        return ResponseEntity.ok("User role updated successfully.");
    }

    /**
     * Get user details by ID
     */
    @GetMapping("${api.admin.getById}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<UserDTO> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


}
