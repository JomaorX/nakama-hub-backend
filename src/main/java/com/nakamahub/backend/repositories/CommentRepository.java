package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Listar todos los comentarios de un post, ordenados por fecha
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // Listar todos los comentarios de un usuario
    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
