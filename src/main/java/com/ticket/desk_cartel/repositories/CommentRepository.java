package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Comment;
import com.ticket.desk_cartel.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicket(Ticket ticket);
    List<Comment> findByReplyToComment(Comment replyToCommentId);
}
