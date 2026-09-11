package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.models.Category;
import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PostMapper {

    private final PostRepository postRepository;

    public PostMapper(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /** Sin visitante: para respuestas donde no hay sesión o no importa el estado personal. */
    public PostResponseDTO toDTO(Post post) {
        return toDTO(post, null, Set.of());
    }

    public PostResponseDTO toDTO(Post post, User viewer) {
        return toDTO(post, viewer, likedIds(viewer, List.of(post)));
    }

    /**
     * Convierte una página resolviendo de una sola vez qué posts ha marcado el
     * visitante, en lugar de preguntarlo uno a uno.
     */
    public Page<PostResponseDTO> toPage(Page<Post> posts, User viewer) {
        Set<Long> liked = likedIds(viewer, posts.getContent());
        return posts.map(post -> toDTO(post, viewer, liked));
    }

    public List<PostResponseDTO> toList(List<Post> posts, User viewer) {
        Set<Long> liked = likedIds(viewer, posts);
        return posts.stream().map(post -> toDTO(post, viewer, liked)).toList();
    }

    private Set<Long> likedIds(User viewer, List<Post> posts) {
        if (viewer == null || posts.isEmpty()) {
            return Set.of();
        }
        return postRepository.findLikedPostIds(viewer.getId(), posts.stream().map(Post::getId).toList());
    }

    private PostResponseDTO toDTO(Post post, User viewer, Set<Long> likedPostIds) {
        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categories(post.getCategories().stream().map(Category::getName).toList())
                .authorUsername(post.getAuthor().getUsername())
                .serieName(post.getSerie() != null ? post.getSerie().getName() : null)
                .contentType(post.getContentType())
                // Copia defensiva: getImageUrls devuelve la colección perezosa de Hibernate,
                // que ya no se puede inicializar cuando Jackson serializa fuera de la transacción.
                .imageUrls(List.copyOf(post.getImageUrls()))
                .status(post.getStatus())
                .privacy(post.getPrivacy())
                .viewsCount(post.getViewsCount())
                .likesCount(post.getLikesCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .edited(post.getEditedAt() != null)
                .likedByMe(likedPostIds.contains(post.getId()))
                .own(viewer != null && post.getAuthor().equals(viewer))
                .build();
    }
}
