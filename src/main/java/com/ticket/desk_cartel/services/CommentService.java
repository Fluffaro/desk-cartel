package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Comment;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.CommentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import jakarta.security.auth.message.AuthException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }


    public Comment addComment(String text, Long replyToCommentId, Long ticketId, Long userId) throws Exception {

        Optional<Ticket> findTicket = ticketRepository.findById(ticketId);
        Optional<User> findUser = userRepository.findById(userId);

        if(findTicket.isEmpty() || findUser.isEmpty()) {
            throw new Exception("Ticket or User does not exist.");
        }

        Ticket ticket = findTicket.get();
        User author = findUser.get();

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setText(text);


        if(replyToCommentId != null){
            Optional<Comment> findReplyComment = commentRepository.findById(replyToCommentId);

            if(findReplyComment.isEmpty()) {
                throw new Exception("Comment to reply is missing!");
            }

            Comment replyToComment = findReplyComment.get();
            comment.setReplyToComment(replyToComment);

        }

        comment.setComment_timestamp(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByTicketId(Long ticketId) throws Exception{
        Optional<Ticket> findTicket = ticketRepository.findById(ticketId);

        if(findTicket.isEmpty()){
            throw new Exception("Ticket do not exist");
        }
        Ticket ticket = findTicket.get();
        return commentRepository.findByTicket(ticket);
    }

    public Comment editComment(Long commentId, String text) throws Exception {
        Optional<Comment> findComment = commentRepository.findById(commentId);

        if(findComment.isEmpty()) {
            throw new Exception("Comment does not exist");
        }

        Comment comment = findComment.get();

        comment.setText(text);
        return commentRepository.save(comment);
    }


}
