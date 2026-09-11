package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.models.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponseDTO toDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .edited(PostMapper.isEdited(comment.getCreatedAt(), comment.getUpdatedAt()))
                .build();
    }
}
