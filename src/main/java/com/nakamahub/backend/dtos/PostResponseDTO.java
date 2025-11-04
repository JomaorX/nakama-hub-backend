package com.nakamahub.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostResponseDTO {

    private Long id;

    private String title;

    private String content;

    private String authorUsername;

    private LocalDateTime CreatedAt;

    List<String> categories;
}
