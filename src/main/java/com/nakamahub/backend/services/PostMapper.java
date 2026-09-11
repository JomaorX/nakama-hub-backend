package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.models.Category;
import com.nakamahub.backend.models.Post;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PostMapper {

    /** Margen para absorber la diferencia entre las dos marcas de tiempo del alta. */
    static final Duration EDIT_THRESHOLD = Duration.ofSeconds(1);

    static boolean isEdited(LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt == null || updatedAt == null) {
            return false;
        }
        return Duration.between(createdAt, updatedAt).compareTo(EDIT_THRESHOLD) > 0;
    }

    public PostResponseDTO toDTO(Post post) {
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
                .edited(isEdited(post.getCreatedAt(), post.getUpdatedAt()))
                .build();
    }
}
