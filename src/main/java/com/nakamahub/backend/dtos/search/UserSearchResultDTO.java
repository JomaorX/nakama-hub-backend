package com.nakamahub.backend.dtos.search;

import lombok.Builder;

/** Ficha reducida de usuario para los resultados de búsqueda. Nunca incluye el email. */
@Builder
public record UserSearchResultDTO(
        Long id,
        String username,
        String bio,
        String avatarUrl,
        int followersCount,
        int reputationPoints
) {
}
