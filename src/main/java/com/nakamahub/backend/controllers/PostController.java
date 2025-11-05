package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.CreatePostDTO;
import com.nakamahub.backend.dtos.PostResponseDTO;
import com.nakamahub.backend.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    PostService postService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public List<PostResponseDTO> getPosts (){
        return  postService.getAllPost();
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
}
