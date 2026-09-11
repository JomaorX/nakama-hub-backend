package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.post.UpdatePostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponseDTO createPost(CreatePostDTO createPostDTO, String username);

    PostResponseDTO updatePost(Long id, UpdatePostDTO updatePostDTO, String username);

    /** @param viewerUsername null en peticiones anónimas. */
    Page<PostResponseDTO> getAllPost(Pageable pageable, String viewerUsername);

    /** Timeline de las cuentas que sigue el usuario, más lo suyo propio. Requiere autenticación. */
    Page<PostResponseDTO> getFollowingFeed(Pageable pageable, String viewerUsername);

    /** @param viewerUsername null en peticiones anónimas. */
    PostResponseDTO getPostById(Long id, String viewerUsername);

    PostResponseDTO toggleLike(Long id, String username);

    void deletePost(Long id, String currentUsername);

    void deletePostAsAuthority(Long id);
}
