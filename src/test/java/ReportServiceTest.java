import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.*;
import com.ticket.desk_cartel.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;
import java.io.*;

public class ReportServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketService ticketService;

    private ReportService reportService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        reportService = new ReportService(ticketRepository, agentRepository, userRepository);
        reportService.ticketService = ticketService; // Injecting mock TicketService
    }

    @Test
    public void testGenerateTicketsReport() throws Exception {
        // Given: Mock ticket data
        Category category = new Category("Technical");
        Priority priority = new Priority("HIGH", 3, 24);
        Status status = Status.OPEN;

        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setTitle("Ticket 1");
        ticket1.setPriority(priority);
        ticket1.setStatus(status);
        ticket1.setCategory(category);
        ticket1.setDate_started(LocalDateTime.now().minusDays(1));

        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2L);
        ticket2.setTitle("Ticket 2");
        ticket2.setPriority(priority);
        ticket2.setStatus(status);
        ticket2.setCategory(category);
        ticket2.setDate_started(LocalDateTime.now().minusDays(2));

        List<Ticket> tickets = Arrays.asList(ticket1, ticket2);

        // When: The filterTickets method is called to fetch tickets
        when(ticketService.filterTickets(category, priority, status)).thenReturn(tickets);

        // When: The generateTicketsReport method is called
        byte[] pdfReport = reportService.generateTicketsReport(category, priority, status);

        // Then: Verify that the PDF report is generated
        assertNotNull(pdfReport, "PDF report should not be null");
        assertTrue(pdfReport.length > 0, "PDF report should not be empty");
    }

    @Test
    public void testGenerateTicketsReportWithNoFilters() throws Exception {
        // Given: Mock ticket data
        List<Ticket> tickets = Arrays.asList(new Ticket(), new Ticket());

        // When: The generateTicketsReport method is called with no filters
        when(ticketService.filterTickets(null, null, null)).thenReturn(tickets);
        byte[] pdfReport = reportService.generateTicketsReport(null, null, null);

        // Then: Verify that the PDF report is generated
        assertNotNull(pdfReport, "PDF report should not be null");
        assertTrue(pdfReport.length > 0, "PDF report should not be empty");
    }

    @Test
    public void testGenerateTicketsReportWithEmptyTicketList() throws Exception {
        // Given: Empty list of tickets
        List<Ticket> tickets = Collections.emptyList();

        // When: The generateTicketsReport method is called with no tickets
        when(ticketService.filterTickets(null, null, null)).thenReturn(tickets);
        byte[] pdfReport = reportService.generateTicketsReport(null, null, null);

        // Then: Verify that the PDF report is generated, but the list is empty
        assertNotNull(pdfReport, "PDF report should not be null");
        assertTrue(pdfReport.length > 0, "PDF report should not be empty");
    }

    @Test
    public void testGenerateTicketsReportWithNullCategory() throws Exception {
        // Given: Mock ticket data with null category
        Priority priority = new Priority("HIGH", 3, 24);
        Status status = Status.OPEN;

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTitle("Ticket 1");
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setCategory(null);
        ticket.setDate_started(LocalDateTime.now().minusDays(1));

        List<Ticket> tickets = Arrays.asList(ticket);

        // When: The generateTicketsReport method is called with null category
        when(ticketService.filterTickets(null, priority, status)).thenReturn(tickets);
        byte[] pdfReport = reportService.generateTicketsReport(null, priority, status);

        // Then: Verify that the PDF report is generated
        assertNotNull(pdfReport, "PDF report should not be null");
        assertTrue(pdfReport.length > 0, "PDF report should not be empty");
    }
}
