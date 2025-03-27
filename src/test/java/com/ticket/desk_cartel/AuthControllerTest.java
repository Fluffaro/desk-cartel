package com.ticket.desk_cartel;

import com.ticket.desk_cartel.controllers.AuthController;
import com.ticket.desk_cartel.dto.RegisterRequestDTO;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.services.AuthService;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private RegisterRequestDTO registerRequest;

    @BeforeEach
    void setUp() {
        // Initialize registerRequest with valid values
        registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("testuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");
    }

    @Test
    void testRegister_success() {
        // Arrange: Mock the service behavior
        User mockUser = new User();
        mockUser.setUsername("testuser");

        // Mock the register method to return the mock user
        when(authService.register(any(), any(), any(), any(), any(), any(), any())).thenReturn(mockUser);

        // Act: Call the controller's register method
        ResponseEntity<?> response = authController.register(registerRequest);

        // Assert: Verify the response status and body (assuming it's a Map)
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // Check if the response body is a Map (this may vary depending on implementation)
        if (response.getBody() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> responseBody = (Map<String, String>) response.getBody();
            assertThat(responseBody).containsEntry("message", "User registered successfully. Please verify your email.");
        } else if (response.getBody() instanceof String) {
            // If the response body is just a string
            assertThat(response.getBody()).isEqualTo("User registered successfully. Please verify your email.");
        } else {
            // Handle other cases if any
            throw new AssertionError("Unexpected response body type");
        }
    }

    @Test
    void testRegister_missingFields() {
        // Arrange: Create a RegisterRequestDTO with missing fields
        RegisterRequestDTO incompleteRequest = new RegisterRequestDTO();
        incompleteRequest.setUsername("testuser");
        incompleteRequest.setEmail(null); // Missing email

        // Act: Call the controller's register method
        ResponseEntity<?> response = authController.register(incompleteRequest);

        // Assert: Verify the response status and body (expecting a 400 Bad Request)
        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Missing required fields: username, email, fullName, or password.");
    }

    // Add more tests for other edge cases (e.g., failed registration, invalid input)
}
