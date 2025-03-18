package com.ticket.desk_cartel.repositories;

import com.ticket.desk_cartel.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketId(Long ticketId);
    List<Comment> findByReplyToCommentById(Long replyToCommentId);
}
