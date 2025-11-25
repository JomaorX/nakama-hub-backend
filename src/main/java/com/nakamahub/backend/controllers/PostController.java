package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.CreatePostDTO;
import com.nakamahub.backend.dtos.PostResponseDTO;
import com.nakamahub.backend.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    PostService postService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public Page<PostResponseDTO> getPosts (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return  postService.getAllPost(pageable);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PostResponseDTO getPostById (@PathVariable Long id){
        return  postService.getPostById(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponseDTO createPost (@RequestBody CreatePostDTO body){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return postService.createPost(body, username);
    }

    @PostMapping("/{id}/like")
    @ResponseStatus(HttpStatus.OK)
    public PostResponseDTO likePost (@PathVariable Long id){
        return postService.likePost(id);
    }
}
