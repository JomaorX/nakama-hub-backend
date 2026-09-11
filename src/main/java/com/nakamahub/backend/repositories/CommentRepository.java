package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "post"})
    Page<Comment> findByPostIdAndParentIdIsNull(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post"})
    Page<Comment> findByParentId(Long parentId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "parent"})
    Optional<Comment> findWithAuthorById(Long id);

    /** Todos los comentarios del usuario, sin filtro de visibilidad: solo para su propia exportación de datos. */
    @EntityGraph(attributePaths = {"author", "post", "parent"})
    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /**
     * Comentarios de un autor, limitados a los posts que el visitante puede ver.
     * Sin este filtro el historial de comentarios de un usuario delataba el contenido
     * de posts privados y de borradores ajenos.
     */
    @EntityGraph(attributePaths = {"author", "post"})
    @Query("""
            select c
            from Comment c
            join c.post p
            where c.author.id = :authorId
              and (p.author.id = :viewerId
                   or (p.status = com.nakamahub.backend.models.PostStatus.PUBLISHED
                       and (p.privacy = com.nakamahub.backend.models.PrivacyLevel.PUBLIC
                            or (p.privacy = com.nakamahub.backend.models.PrivacyLevel.FOLLOWERS_ONLY
                                and exists (select 1
                                            from User postAuthor
                                            join postAuthor.followers follower
                                            where postAuthor.id = p.author.id
                                              and follower.id = :viewerId)))))
            """)
    Page<Comment> findVisibleByAuthorId(@Param("authorId") Long authorId,
                                        @Param("viewerId") Long viewerId,
                                        Pageable pageable);
}
