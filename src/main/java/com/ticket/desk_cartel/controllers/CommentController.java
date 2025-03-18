package com.ticket.desk_cartel.controllers;


import com.ticket.desk_cartel.entities.Comment;
import com.ticket.desk_cartel.services.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<Comment> addComment (
            @RequestParam Long ticketId,
            @RequestParam Long userId,
            @RequestParam String text,
            @RequestParam(required = false) Long replyToCommentId
            ) {
        Comment comment = commentService.addComment(ticketId, userId, text, replyToCommentId);

        if(comment == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(comment);
    }

    @GetMapping
    public ResponseEntity<List<Comment>> getCommentsByTicket (
            @RequestParam Long ticketId
    ){
        List<Comment> comments = commentService.getCommentsByTicketId(ticketId);
        return ResponseEntity.ok(comments);
    }
}
