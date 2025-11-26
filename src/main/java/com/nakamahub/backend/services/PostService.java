package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PostService {
    PostResponseDTO createPost (CreatePostDTO createPostDTO, String username);
    Page<PostResponseDTO> getAllPost(Pageable pageable);
    PostResponseDTO getPostById(Long id);
    PostResponseDTO toggleLike(Long id, String username);
    void deletePost (Long id, String currentUsername);
}
