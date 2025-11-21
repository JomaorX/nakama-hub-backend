package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.CommentResponseDTO;
import com.nakamahub.backend.dtos.CreateCommentDTO;
import com.nakamahub.backend.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    CommentService commentService;

    @GetMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponseDTO> getPostComments (@PathVariable Long postId){
        return commentService.getCommentsByPost(postId);
    }

    @GetMapping("/parent/{parentId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponseDTO> getReplies(@PathVariable Long parentId) {
        return commentService.getCommentsByParent(parentId);
    }

    @GetMapping("/user/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponseDTO> getUserComments(@PathVariable Long authorId) {
        return commentService.getCommentsByUser(authorId);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO createComment (@Valid @RequestBody CreateCommentDTO newComment){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return commentService.createComment(newComment, username);
    }

}
