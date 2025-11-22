package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CommentResponseDTO;
import com.nakamahub.backend.dtos.CreateCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CommentService {

    CommentResponseDTO createComment (CreateCommentDTO createCommentDTO, String username);

    Page<CommentResponseDTO> getCommentsByPost (Long postId, Pageable pageable);

    Page<CommentResponseDTO> getCommentsByUser(Long authorId, Pageable pageable);

    Page<CommentResponseDTO> getCommentsByParent (Long parentId, Pageable pageable);
}
