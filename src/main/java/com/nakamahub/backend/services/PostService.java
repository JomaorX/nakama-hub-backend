package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponseDTO createPost(CreatePostDTO createPostDTO, String username);

    /** @param viewerUsername null en peticiones anónimas. */
    Page<PostResponseDTO> getAllPost(Pageable pageable, String viewerUsername);

    /** @param viewerUsername null en peticiones anónimas. */
    PostResponseDTO getPostById(Long id, String viewerUsername);

    PostResponseDTO toggleLike(Long id, String username);

    void deletePost(Long id, String currentUsername);

    void deletePostAsAuthority(Long id);
}
