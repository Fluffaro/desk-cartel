import com.ticket.desk_cartel.entities.Comment;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.CommentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import com.ticket.desk_cartel.services.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private Comment comment;
    private Ticket ticket;
    private User user;

    @BeforeEach
    void setUp() {
        // Create instances using constructors or builder methods if available
        ticket = new Ticket(); // If there's no setId(), check if there's a constructor that takes an ID
        user = new User();
        comment = new Comment();

        // Set the ID properly using direct assignment (if accessible)
        setEntityId(ticket, 1L);
        setEntityId(user, 1L);
        setEntityId(comment, 1L);

        comment.setTicket(ticket);
        comment.setAuthor(user);
        comment.setText("This is a comment");
        comment.setComment_timestamp(LocalDateTime.now());
    }

    private void setEntityId(Object entity, Long id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id"); // Check superclass for ID field
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException e) {
            System.err.println("Field 'id' not found in " + entity.getClass());
        } catch (IllegalAccessException e) {
            System.err.println("Cannot access field 'id' in " + entity.getClass());
        }
    }

    @Test
    void testAddComment_Success() throws Exception {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment savedComment = commentService.addComment("This is a comment", null, 1L, 1L);

        assertNotNull(savedComment);
        assertEquals("This is a comment", savedComment.getText());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void testGetCommentsByTicketId_Success() throws Exception {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket(ticket)).thenReturn(List.of(comment));

        List<Comment> comments = commentService.getCommentsByTicketId(1L);

        assertFalse(comments.isEmpty());
        assertEquals(1, comments.size());
    }

    @Test
    void testEditComment_Success() throws Exception {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment updatedComment = commentService.editComment(1L, "Updated comment");

        assertEquals("Updated comment", updatedComment.getText());
        verify(commentRepository).save(any(Comment.class));
    }
}
