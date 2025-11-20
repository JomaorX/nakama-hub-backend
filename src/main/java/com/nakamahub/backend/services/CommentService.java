package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CommentResponseDTO;
import com.nakamahub.backend.dtos.CreateCommentDTO;

import java.util.List;

public interface CommentService {

    CommentResponseDTO createComment (CreateCommentDTO createCommentDTO, String username);

    List<CommentResponseDTO> getCommentsByPost (Long postId);

    List<CommentResponseDTO> getCommentsByUser(Long authorId);

    List<CommentResponseDTO> getCommentsByParent (Long parentId);
}
