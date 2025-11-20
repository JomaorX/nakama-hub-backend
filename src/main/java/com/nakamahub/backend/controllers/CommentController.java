package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.CommentResponseDTO;
import com.nakamahub.backend.dtos.CreateCommentDTO;
import com.nakamahub.backend.services.CommentService;
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

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponseDTO> getPostComments (@PathVariable Long postId){
        return commentService.getCommentsByPost(postId);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO createComment (@RequestBody CreateCommentDTO newComment){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return commentService.createComment(newComment, username);
    }

}
