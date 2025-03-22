package com.ticket.desk_cartel.controllers;


import com.ticket.desk_cartel.entities.Comment;
import com.ticket.desk_cartel.services.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.comment.base-url}")
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
            ) throws Exception {
        Comment comment = commentService.addComment(text,replyToCommentId, ticketId, userId  );

        if(comment == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(comment);
    }

    @GetMapping("${api.comment.ticketId}")
    public ResponseEntity<List<Comment>> getCommentsByTicket (
            @PathVariable Long ticketId
    ) throws Exception {
        List<Comment> comments = commentService.getCommentsByTicketId(ticketId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("${api.comment.commentId}")
    public ResponseEntity<Comment> editComment (@PathVariable Long commentId, @RequestParam String text) throws Exception {
        System.out.println("Received commentId: " + commentId + " and text: " + text);
        Comment comment = commentService.editComment(commentId, text);
        return ResponseEntity.ok(comment);
    }
}
