package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.models.Category;
import com.nakamahub.backend.models.Post;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {

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
                .build();
    }
}
