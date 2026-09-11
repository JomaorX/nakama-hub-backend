package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Los filtros de búsqueda son todos opcionales. Escribirlo en JPQL obligaría a
 * repetir el patrón ":parametro is null or ..." en cada rama, que impide a la base
 * de datos aprovechar los índices y es difícil de leer. Con la API de criterios solo
 * se añade la condición cuando el filtro viene informado.
 */
public class PostSearchRepositoryFragmentImpl implements PostSearchRepositoryFragment {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Post> search(Long viewerId, String text, ContentType contentType,
                             String serieName, String category, Pageable pageable) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Post> query = builder.createQuery(Post.class);
        Root<Post> post = query.from(Post.class);
        // Evita una consulta por fila para el autor y la serie al construir el DTO.
        post.fetch("author", JoinType.LEFT);
        post.fetch("serie", JoinType.LEFT);

        query.where(builder.and(
                predicates(builder, query, post, viewerId, text, contentType, serieName, category)));
        query.orderBy(orderBy(builder, post, pageable.getSort()));

        List<Post> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(content, pageable,
                count(builder, viewerId, text, contentType, serieName, category));
    }

    private long count(CriteriaBuilder builder, Long viewerId, String text, ContentType contentType,
                       String serieName, String category) {
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<Post> post = query.from(Post.class);
        query.select(builder.count(post));
        query.where(builder.and(
                predicates(builder, query, post, viewerId, text, contentType, serieName, category)));
        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate[] predicates(CriteriaBuilder builder, CriteriaQuery<?> query, Root<Post> post,
                                   Long viewerId, String text, ContentType contentType,
                                   String serieName, String category) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(visibleTo(builder, query, post, viewerId));

        if (text != null && !text.isBlank()) {
            // LIKE con comodín por delante no puede usar un índice. Para el tamaño de una
            // comunidad pequeña es suficiente; el paso siguiente sería un índice FULLTEXT.
            String pattern = "%" + text.toLowerCase().trim() + "%";
            predicates.add(builder.or(
                    builder.like(builder.lower(post.get("title")), pattern),
                    builder.like(builder.lower(post.get("content")), pattern)));
        }
        if (contentType != null) {
            predicates.add(builder.equal(post.get("contentType"), contentType));
        }
        if (serieName != null && !serieName.isBlank()) {
            predicates.add(builder.equal(post.get("serie").get("name"), serieName));
        }
        if (category != null && !category.isBlank()) {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Post> self = subquery.from(Post.class);
            Join<Post, Category> categories = self.join("categories");
            subquery.select(builder.literal(1L))
                    .where(builder.equal(self.get("id"), post.get("id")),
                           builder.equal(categories.get("name"), category));
            predicates.add(builder.exists(subquery));
        }

        return predicates.toArray(new Predicate[0]);
    }

    /** Mismas reglas que PostRepository.findVisibleFor y que PostVisibility. */
    private Predicate visibleTo(CriteriaBuilder builder, CriteriaQuery<?> query, Root<Post> post, Long viewerId) {
        Predicate published = builder.equal(post.get("status"), PostStatus.PUBLISHED);

        if (viewerId == null) {
            return builder.and(published, builder.equal(post.get("privacy"), PrivacyLevel.PUBLIC));
        }

        Subquery<Long> follows = query.subquery(Long.class);
        Root<User> author = follows.from(User.class);
        Join<User, User> followers = author.join("followers");
        follows.select(builder.literal(1L))
                .where(builder.equal(author.get("id"), post.get("author").get("id")),
                       builder.equal(followers.get("id"), viewerId));

        return builder.or(
                builder.equal(post.get("author").get("id"), viewerId),
                builder.and(published, builder.or(
                        builder.equal(post.get("privacy"), PrivacyLevel.PUBLIC),
                        builder.and(
                                builder.equal(post.get("privacy"), PrivacyLevel.FOLLOWERS_ONLY),
                                builder.exists(follows)))));
    }

    private List<Order> orderBy(CriteriaBuilder builder, Root<Post> post, Sort sort) {
        if (sort.isEmpty()) {
            return List.of(builder.desc(post.get("createdAt")));
        }
        return sort.stream()
                .map(order -> order.isAscending()
                        ? builder.asc(post.get(order.getProperty()))
                        : builder.desc(post.get(order.getProperty())))
                .map(Order.class::cast)
                .toList();
    }
}
