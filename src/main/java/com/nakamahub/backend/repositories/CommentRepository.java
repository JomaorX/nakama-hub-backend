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

    /** Comentarios de primer nivel de un post, sin los de cuentas bloqueadas. */
    @EntityGraph(attributePaths = {"author", "post"})
    @Query("""
            select c
            from Comment c
            where c.post.id = :postId
              and c.parent is null
              and """ + BlockQueries.NOT_BLOCKED_WITH_COMMENT_AUTHOR)
    Page<Comment> findThreadStarters(@Param("postId") Long postId,
                                     @Param("viewerId") Long viewerId,
                                     Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post"})
    @Query("""
            select c
            from Comment c
            where c.parent.id = :parentId
              and """ + BlockQueries.NOT_BLOCKED_WITH_COMMENT_AUTHOR)
    Page<Comment> findReplies(@Param("parentId") Long parentId,
                              @Param("viewerId") Long viewerId,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "parent"})
    Optional<Comment> findWithAuthorById(Long id);

    /** Todos los comentarios del usuario, sin filtros: solo para su propia exportación de datos. */
    @EntityGraph(attributePaths = {"author", "post", "parent"})
    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /**
     * Número de respuestas de cada comentario indicado, en una sola consulta.
     * Pedirlo uno a uno convertiría una página de veinte comentarios en veintiuna
     * consultas.
     */
    @Query("select c.parent.id, count(c) from Comment c where c.parent.id in :parentIds group by c.parent.id")
    List<Object[]> countRepliesFor(@Param("parentIds") List<Long> parentIds);

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
              and """ + BlockQueries.NOT_BLOCKED_WITH_COMMENT_AUTHOR)
    Page<Comment> findVisibleByAuthorId(@Param("authorId") Long authorId,
                                        @Param("viewerId") Long viewerId,
                                        Pageable pageable);
}
