package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.dtos.comment.CreateCommentDTO;
import com.nakamahub.backend.dtos.comment.UpdateCommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponseDTO createComment(CreateCommentDTO createCommentDTO, String username);

    /** @param viewerUsername null en peticiones anónimas. */
    Page<CommentResponseDTO> getCommentsByPost(Long postId, Pageable pageable, String viewerUsername);

    /** @param viewerUsername null en peticiones anónimas. */
    Page<CommentResponseDTO> getCommentsByUser(Long authorId, Pageable pageable, String viewerUsername);

    /** @param viewerUsername null en peticiones anónimas. */
    Page<CommentResponseDTO> getCommentsByParent(Long parentId, Pageable pageable, String viewerUsername);

    CommentResponseDTO updateComment(Long commentId, UpdateCommentDTO updateCommentDTO, String authorUsername);

    void deleteComment(Long commentId, String authorUsername);

    void deleteCommentAsAuthority(Long id);
}
