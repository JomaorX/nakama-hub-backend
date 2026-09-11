package com.nakamahub.backend.dtos.user;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Volcado de todo lo que la plataforma guarda sobre un usuario.
 *
 * El RGPD reconoce el derecho de acceso y a la portabilidad de los datos, así que
 * una comunidad con usuarios reales tiene que poder entregar esto sin intervención
 * manual. Solo lo puede pedir el propio interesado.
 */
@Builder
public record UserDataExportDTO(
        Instant generatedAt,
        Account account,
        List<String> followers,
        List<String> following,
        List<String> likedPostTitles,
        List<PostResponseDTO> posts,
        List<CommentResponseDTO> comments
) {

    @Builder
    public record Account(
            Long id,
            String username,
            String email,
            String bio,
            String avatarUrl,
            String role,
            String privacy,
            String status,
            int reputationPoints
    ) {
    }
}
