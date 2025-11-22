package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsById(Long id);

    Page<Comment> findByPostIdAndParentIdIsNull(Long postId, Pageable pageable);

    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);

    Page<Comment> findByParentId(Long parentId, Pageable pageable);
}
