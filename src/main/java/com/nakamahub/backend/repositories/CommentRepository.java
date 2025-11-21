package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsById(Long id);

    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId);

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
