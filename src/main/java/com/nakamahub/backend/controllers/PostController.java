package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.security.SecurityUtils;
import com.nakamahub.backend.services.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    /** Tope de página para que nadie pida el feed entero de una vez. */
    private static final int MAX_PAGE_SIZE = 50;

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public Page<PostResponseDTO> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), Sort.by("createdAt").descending());
        return postService.getAllPost(pageable, SecurityUtils.currentUsername().orElse(null));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PostResponseDTO getPostById(@PathVariable Long id) {
        return postService.getPostById(id, SecurityUtils.currentUsername().orElse(null));
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponseDTO createPost(@Valid @RequestBody CreatePostDTO body) {
        return postService.createPost(body, SecurityUtils.requireCurrentUsername());
    }

    @PostMapping("/{id}/like")
    @ResponseStatus(HttpStatus.OK)
    public PostResponseDTO toggleLike(@PathVariable Long id) {
        return postService.toggleLike(id, SecurityUtils.requireCurrentUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id) {
        postService.deletePost(id, SecurityUtils.requireCurrentUsername());
    }

    @DeleteMapping("/{id}/authority")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePostAsAuthority(@PathVariable Long id) {
        postService.deletePostAsAuthority(id);
    }

    private static int clampSize(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}
