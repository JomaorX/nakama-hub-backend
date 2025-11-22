package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CreatePostDTO;
import com.nakamahub.backend.dtos.PostResponseDTO;

import java.util.List;


public interface PostService {
    PostResponseDTO createPost (CreatePostDTO createPostDTO, String username);
    List<PostResponseDTO> getAllPost();
    PostResponseDTO getPostById(Long id);
}
