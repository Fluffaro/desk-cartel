package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Comment;
import com.ticket.desk_cartel.entities.Ticket;
import com.ticket.desk_cartel.entities.User;
import com.ticket.desk_cartel.repositories.CommentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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


    public Comment addComment(Long ticketId, Long userId, String text, Long replyToCommentId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);

        User author = userRepository.findById(userId).orElse(null);

        if(ticket == null || author == null) {
            return null;
        }

        Comment comment = new Comment();
        comment.setTicket_id(ticket);
        comment.setAuthor_id(author);
        comment.setText(text);


        if(replyToCommentId != null){
            Comment replyToComment = commentRepository.findById(replyToCommentId).orElse(null);
            if(replyToComment != null) {
                comment.setReplyToComment(replyToComment);
            }
        }

        comment.setComment_timestamp(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByTicketId(Long ticketId){
        return commentRepository.findByTicketId(ticketId);
    }
}
