package com.nakamahub.backend.dtos;

import com.nakamahub.backend.models.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PostResponseDTO {

    private Long id;

    private String title;

    private String content;

    private ContentType contentType;

    private String serieName;

    List<String> categories;

    private String authorUsername;
}
