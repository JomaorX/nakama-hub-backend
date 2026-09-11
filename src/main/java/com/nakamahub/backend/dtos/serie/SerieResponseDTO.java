package com.nakamahub.backend.dtos.serie;

import lombok.Builder;

@Builder
public record SerieResponseDTO(
        Long id,
        String name,
        String description
) {
}
