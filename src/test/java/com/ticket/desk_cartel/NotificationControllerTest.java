package com.ticket.desk_cartel;

import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.services.NotificationService;
import com.ticket.desk_cartel.services.AgentService;
import com.ticket.desk_cartel.controllers.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private AgentService agentService;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    private Agent agent;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc and inject mocks
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();

        // Create and set up an agent for testing
        agent = new Agent();
        agent.setId(1L);
        agent.setNotifCount(5);  // Assume the agent has 5 notifications
    }

    @Test
    void testGetNotifCount_Success() throws Exception {
        // Mock the agentRepository to return an agent
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));

        // Perform GET request and verify the response
        mockMvc.perform(get("/api/notifications/notifCount/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(agent.getNotifCount()));

        // Verify repository call was made once
        verify(agentRepository, times(1)).findById(1L);
    }



    @Test
    void testAgentClickedNotification() throws Exception {
        // Perform PUT request for agent clicked notification
        mockMvc.perform(put("/api/notifications/clickedNotification")
                        .param("id", "1"))
                .andExpect(status().isOk());

        // Verify service method was called once
        verify(notificationService, times(1)).clickedAgentNotification(1L);
    }

    @Test
    void testUserClickedNotification() throws Exception {
        // Perform PUT request for user clicked notification
        mockMvc.perform(put("/api/notifications/clickedNotification/{id}", 1L))
                .andExpect(status().isOk());

        // Verify service method was called once
        verify(notificationService, times(1)).clickedUserNotification(1L);
    }

    @Test
    void testGetAgentNotificationCount() throws Exception {
        // Mock the service method to return a notification count for agent
        when(notificationService.getAgentNumbersOfNotifications(1L)).thenReturn(10);

        // Perform GET request and verify the response
        mockMvc.perform(get("/api/notifications/NotificationCount")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10));

        // Verify service call was made once
        verify(notificationService, times(1)).getAgentNumbersOfNotifications(1L);
    }

    @Test
    void testGetUserNotificationCount() throws Exception {
        // Mock the service method to return a notification count for user
        when(notificationService.getUserNumbersOfNotifications(1L)).thenReturn(10);

        // Perform GET request and verify the response
        mockMvc.perform(get("/api/notifications/NotificationCount/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10));

        // Verify service call was made once
        verify(notificationService, times(1)).getUserNumbersOfNotifications(1L);
    }


}
