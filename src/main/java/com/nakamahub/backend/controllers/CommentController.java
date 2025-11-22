package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.CommentResponseDTO;
import com.nakamahub.backend.dtos.CreateCommentDTO;
import com.nakamahub.backend.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    CommentService commentService;

    @GetMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDTO> getPostComments (
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return commentService.getCommentsByPost(postId, pageable);
    }

    @GetMapping("/parent/{parentId}")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDTO> getReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return commentService.getCommentsByParent(parentId, pageable);
    }

    @GetMapping("/user/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDTO> getUserComments(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return commentService.getCommentsByUser(authorId, pageable);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO createComment (@Valid @RequestBody CreateCommentDTO newComment){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return commentService.createComment(newComment, username);
    }

}
