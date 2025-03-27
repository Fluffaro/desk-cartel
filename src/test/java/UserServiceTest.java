import com.ticket.desk_cartel.dto.UserDTO;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.entities.Agent;
import com.ticket.desk_cartel.entities.AgentLevel;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Agent testAgent;
    private String username = "testUser";
    private String password = "password123";
    private String encodedPassword = "$2a$10$z.M9.Vu0F9pTPF..xZn9lu.RCxfA0BZReQW6Vn3TpBpkNfsQkh6U2"; // BCrypt encoded password for "password123"

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(username);
        testUser.setPassword(encodedPassword);
        testUser.setRole("USER");
        testUser.setActive(true);
        testUser.setVerified(true);
        testUser.setEmail("testuser@example.com");
        testUser.setFullName("Test User");
        testUser.setPhoneNumber("1234567890");
        testUser.setAddress("123 Test St");
        testUser.setCreatedAt(LocalDateTime.now());

        testAgent = new Agent(testUser, AgentLevel.JUNIOR);
        testAgent.setActive(true);
    }

    @Test
    void testFindById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> user = userService.findById(1L);

        // Assert
        assertTrue(user.isPresent());
        assertEquals(testUser, user.get());
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<User> user = userService.findById(1L);

        // Assert
        assertFalse(user.isPresent());
    }

    @Test
    void testFindByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> user = userService.findByUsername(username);

        // Assert
        assertTrue(user.isPresent());
        assertEquals(testUser, user.get());
    }

    @Test
    void testFindByUsername_NotFound() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        Optional<User> user = userService.findByUsername(username);

        // Assert
        assertFalse(user.isPresent());
    }

    @Test
    void testUpdateAccountStatus_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.updateAccountStatus(1L, false);

        // Assert
        assertFalse(testUser.isActive());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdateAccountStatus_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateAccountStatus(1L, false);
        });
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userService.loadUserByUsername(username);

        // Assert
        assertEquals(testUser.getUsername(), userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(username);
        });
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testUpdateUserRole_Success_AddAgent() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(agentRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // Act
        userService.updateUserRole(1L, "AGENT");

        // Assert
        assertEquals("AGENT", testUser.getRole());
        verify(agentRepository, times(1)).save(any(Agent.class));
    }

    @Test
    void testUpdateUserRole_Success_RemoveAgent() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(agentRepository.findByUser(testUser)).thenReturn(Optional.of(testAgent));
        when(ticketRepository.countOngoingTickets(testAgent)).thenReturn(0);

        // Act
        userService.updateUserRole(1L, "USER");

        // Assert
        assertEquals("USER", testUser.getRole());
        verify(agentRepository, times(1)).save(testAgent);
    }

    @Test
    void testUpdateUserRole_AgentWithOngoingTickets() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(agentRepository.findByUser(testUser)).thenReturn(Optional.of(testAgent));
        when(ticketRepository.countOngoingTickets(testAgent)).thenReturn(1);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserRole(1L, "USER");
        });
        assertEquals("Cannot remove agent role. The agent has ongoing tickets.", exception.getMessage());
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // Act
        List<UserDTO> userDTOList = userService.getAllUsers();

        // Assert
        assertNotNull(userDTOList);
        assertEquals(1, userDTOList.size());
        assertEquals(testUser.getUsername(), userDTOList.get(0).getUsername());
    }

    @Test
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<UserDTO> userDTO = userService.getUserById(1L);

        // Assert
        assertTrue(userDTO.isPresent());
        assertEquals(testUser.getUsername(), userDTO.get().getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<UserDTO> userDTO = userService.getUserById(1L);

        // Assert
        assertFalse(userDTO.isPresent());
    }
}
