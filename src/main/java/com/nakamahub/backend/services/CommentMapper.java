package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.models.Comment;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommentMapper {

    private final CommentRepository commentRepository;

    public CommentMapper(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public CommentResponseDTO toDTO(Comment comment) {
        return toDTO(comment, null);
    }

    public CommentResponseDTO toDTO(Comment comment, User viewer) {
        return toDTO(comment, viewer, replyCounts(List.of(comment)));
    }

    /**
     * Convierte una página resolviendo el número de respuestas de todos los
     * comentarios en una sola consulta, en lugar de una por comentario.
     */
    public Page<CommentResponseDTO> toPage(Page<Comment> comments, User viewer) {
        Map<Long, Long> counts = replyCounts(comments.getContent());
        return comments.map(comment -> toDTO(comment, viewer, counts));
    }

    private Map<Long, Long> replyCounts(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : commentRepository.countRepliesFor(comments.stream().map(Comment::getId).toList())) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private CommentResponseDTO toDTO(Comment comment, User viewer, Map<Long, Long> replyCounts) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .edited(comment.getEditedAt() != null)
                .replyCount(replyCounts.getOrDefault(comment.getId(), 0L))
                .own(viewer != null && comment.getAuthor().equals(viewer))
                .build();
    }
}
