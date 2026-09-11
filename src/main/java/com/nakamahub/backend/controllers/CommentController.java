package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.dtos.comment.CreateCommentDTO;
import com.nakamahub.backend.security.SecurityUtils;
import com.nakamahub.backend.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDTO createComment(@Valid @RequestBody CreateCommentDTO newComment) {
        return commentService.createComment(newComment, SecurityUtils.requireCurrentUsername());
    }

    @GetMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDTO> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commentService.getCommentsByPost(postId, pageable(page, size, Sort.Direction.ASC),
                SecurityUtils.currentUsername().orElse(null));
    }

    @GetMapping("/{commentId}/replies")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDTO> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commentService.getCommentsByParent(commentId, pageable(page, size, Sort.Direction.ASC),
                SecurityUtils.currentUsername().orElse(null));
    }

    @GetMapping("/user/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDTO> getUserComments(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commentService.getCommentsByUser(authorId, pageable(page, size, Sort.Direction.DESC),
                SecurityUtils.currentUsername().orElse(null));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id, SecurityUtils.requireCurrentUsername());
    }

    @DeleteMapping("/{id}/authority")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentAsAuthority(@PathVariable Long id) {
        commentService.deleteCommentAsAuthority(id);
    }

    private static Pageable pageable(int page, int size, Sort.Direction direction) {
        return PageRequest.of(Math.max(0, page), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(direction, "createdAt"));
    }
}
