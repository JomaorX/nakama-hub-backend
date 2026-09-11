package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostSearchRepositoryFragment {

    boolean existsByTitleAndAuthor(String title, User author);

    /** Igual que el anterior, excluyendo el propio post: hace falta al editarlo. */
    boolean existsByTitleAndAuthorAndIdNot(String title, User author, Long id);

    /**
     * Posts que un visitante concreto puede ver. Antes el feed usaba findAll, de modo
     * que cualquiera, incluso sin autenticar, leía los borradores y los posts privados
     * de todo el mundo.
     *
     * Con viewerId a null las tres comparaciones contra el parámetro dan desconocido,
     * que en SQL no es cierto, así que un anónimo solo ve lo publicado y público.
     */
    @EntityGraph(attributePaths = {"author", "serie"})
    @Query("""
            select p
            from Post p
            where p.author.id = :viewerId
               or (p.status = com.nakamahub.backend.models.PostStatus.PUBLISHED
                   and (p.privacy = com.nakamahub.backend.models.PrivacyLevel.PUBLIC
                        or (p.privacy = com.nakamahub.backend.models.PrivacyLevel.FOLLOWERS_ONLY
                            and exists (select 1
                                        from User author
                                        join author.followers follower
                                        where author.id = p.author.id
                                          and follower.id = :viewerId))))
            """)
    Page<Post> findVisibleFor(@Param("viewerId") Long viewerId, Pageable pageable);

    /**
     * Timeline: lo publicado por las cuentas que el usuario sigue, más lo suyo propio.
     *
     * Se excluyen los borradores y los posts privados, también los del propio autor,
     * porque el timeline es una vista de lectura y no el panel de sus publicaciones.
     * Incluir lo propio evita que una cuenta recién creada vea un muro vacío justo
     * después de publicar.
     */
    @EntityGraph(attributePaths = {"author", "serie"})
    @Query("""
            select p
            from Post p
            where p.status = com.nakamahub.backend.models.PostStatus.PUBLISHED
              and p.privacy <> com.nakamahub.backend.models.PrivacyLevel.PRIVATE
              and (p.author.id = :viewerId
                   or exists (select 1
                              from User author
                              join author.followers follower
                              where author.id = p.author.id
                                and follower.id = :viewerId))
            """)
    Page<Post> findFollowingFeed(@Param("viewerId") Long viewerId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "serie"})
    Optional<Post> findWithAuthorById(Long id);

    /**
     * Incremento atómico. Leer el contador, sumarle uno y guardar la entidad pierde
     * visitas cuando dos peticiones coinciden, que es justo lo que pasa en un feed.
     */
    @Modifying
    @Query("update Post p set p.viewsCount = p.viewsCount + 1 where p.id = :id")
    void incrementViewsCount(@Param("id") Long id);
}
