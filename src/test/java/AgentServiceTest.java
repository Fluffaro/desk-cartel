import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.*;
import com.ticket.desk_cartel.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PriorityRepository priorityRepository;

    private AgentService agentService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        agentService = new AgentService(agentRepository, userRepository, ticketRepository, notificationRepository, verificationTokenService);
        agentService.notificationService = notificationService;  // Inject the mock notificationService
    }

    // Test for creating an agent from a user
    @Test
    public void testCreateAgentSuccessfully() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole("USER");

        Agent agent = new Agent(user, AgentLevel.JUNIOR);

        // Mocking repository calls
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(agentRepository.findByUser(user)).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        // Call the method to create an agent
        Agent createdAgent = agentService.createAgent(1L);

        // Assertions
        assertNotNull(createdAgent);
        assertEquals("AGENT", user.getRole());  // Ensure the user's role is updated
        assertEquals(AgentLevel.JUNIOR, createdAgent.getLevel());
    }

    @Test
    public void testCreateAgentUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the method and assert that it throws an exception
        assertThrows(Exception.class, () -> agentService.createAgent(1L));
    }

    @Test
    public void testCreateAgentAlreadyAgent() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole("USER");

        Agent existingAgent = new Agent(user, AgentLevel.JUNIOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(agentRepository.findByUser(user)).thenReturn(Optional.of(existingAgent));

        // Call the method and assert that it throws an exception
        assertThrows(Exception.class, () -> agentService.createAgent(1L));
    }

    // Test for finding the best agent for a ticket
    @Test
    public void testFindBestAgentForTicketSuccessfully() {
        Priority priority = new Priority("HIGH", 3, 24);

        // Create mock agents
        Agent agent1 = new Agent(new User(), AgentLevel.JUNIOR);
        agent1.setCurrentWorkload(30);
        agent1.setTotalCapacity(100);

        Agent agent2 = new Agent(new User(), AgentLevel.SENIOR);
        agent2.setCurrentWorkload(50);
        agent2.setTotalCapacity(100);

        // Mock repository call to return available agents
        when(agentRepository.findAgentsWithEnoughCapacityFor(priority.getWeight()))
                .thenReturn(Arrays.asList(agent1, agent2));

        // Call the method to find the best agent
        Optional<Agent> bestAgent = agentService.findBestAgentForTicket(priority);

        // Assertions
        assertTrue(bestAgent.isPresent());
        assertEquals(agent1, bestAgent.get());  // Since agent1 has the lower workload percentage
    }

    @Test
    public void testFindBestAgentForTicketNoAgents() {
        Priority priority = new Priority("HIGH", 3, 24);

        // Mock repository call to return no available agents
        when(agentRepository.findAgentsWithEnoughCapacityFor(priority.getWeight()))
                .thenReturn(Collections.emptyList());

        // Call the method to find the best agent
        Optional<Agent> bestAgent = agentService.findBestAgentForTicket(priority);

        // Assertions
        assertFalse(bestAgent.isPresent());
    }

    // Test for assigning a ticket to an agent
    @Test
    @Transactional
    public void testAssignTicketToAgentSuccessfully() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        Priority priority = new Priority("HIGH", 3, 24);
        ticket.setPriority(priority);

        Agent agent = new Agent(new User(), AgentLevel.JUNIOR);
        agent.setCurrentWorkload(30);
        agent.setTotalCapacity(100);

        // Mock repository calls
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(agentRepository.findAgentsWithEnoughCapacityFor(ticket.getPriority().getWeight()))
                .thenReturn(Collections.singletonList(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        // Call the method to assign the ticket
        Ticket updatedTicket = agentService.assignTicketToAgent(1L);

        // Assertions
        assertNotNull(updatedTicket);
        assertEquals(Status.ASSIGNED, updatedTicket.getStatus());
        assertEquals(agent, updatedTicket.getAssignedTicket());
    }

    @Test
    @Transactional
    public void testAssignTicketToAgentNoAvailableAgent() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        Priority priority = new Priority("HIGH", 3, 24);
        ticket.setPriority(priority);

        // Mock repository calls
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(agentRepository.findAgentsWithEnoughCapacityFor(ticket.getPriority().getWeight()))
                .thenReturn(Collections.emptyList());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        // Call the method to assign the ticket
        Ticket updatedTicket = agentService.assignTicketToAgent(1L);

        // Assertions
        assertNotNull(updatedTicket);
        assertEquals(Status.NO_AGENT_AVAILABLE, updatedTicket.getStatus());
        assertNull(updatedTicket.getAssignedTicket());
    }

    @Test
    @Transactional
    public void testAssignTicketToAgentTicketAlreadyAssigned() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        Priority priority = new Priority("HIGH", 3, 24);
        ticket.setPriority(priority);
        Agent agent = new Agent(new User(), AgentLevel.JUNIOR);
        ticket.setAssignedTicket(agent);
        ticket.setStatus(Status.ASSIGNED);

        // Mock repository calls
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Call the method to assign the ticket
        Ticket updatedTicket = agentService.assignTicketToAgent(1L);

        // Assertions
        assertNotNull(updatedTicket);
        assertEquals(Status.ASSIGNED, updatedTicket.getStatus());
        assertEquals(agent, updatedTicket.getAssignedTicket());
    }

    @Test
    @Transactional
    public void testAssignTicketToAgentTicketNotFound() {
        // Mock repository call to return no ticket
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the method to assign the ticket
        Ticket updatedTicket = agentService.assignTicketToAgent(1L);

        // Assertions
        assertNull(updatedTicket);
    }
}
