import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.services.AgentService;
import com.ticket.desk_cartel.services.NotificationService;
import com.ticket.desk_cartel.services.TicketAssignmentScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class TicketAssignmentSchedulerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AgentService agentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TicketAssignmentScheduler ticketAssignmentScheduler;

    private Ticket ticket;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setStatus(Status.NO_AGENT_AVAILABLE);
    }

    @Test
    void testAssignPendingTicketsNoUnassigned() {
        when(ticketRepository.findByStatus(Status.NO_AGENT_AVAILABLE)).thenReturn(Collections.emptyList());
        ticketAssignmentScheduler.assignPendingTickets();
        verify(ticketRepository, times(1)).findByStatus(Status.NO_AGENT_AVAILABLE);

    }

    @Test
    void  testAssignPendingTicketsWithUnassignedTickets() {
        when(ticketRepository.findByStatus(Status.NO_AGENT_AVAILABLE)).thenReturn(Collections.singletonList(ticket));
        when(agentService.assignTicketToAgent(ticket.getTicketId())).thenReturn(ticket);

        ticketAssignmentScheduler.assignPendingTickets();

        verify(agentService, times(1)).assignTicketToAgent(ticket.getTicketId());
        verify(ticketRepository,times(1)).findByStatus(Status.NO_AGENT_AVAILABLE);

    }
}
