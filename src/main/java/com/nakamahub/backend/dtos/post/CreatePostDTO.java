package com.nakamahub.backend.dtos.post;

import com.nakamahub.backend.models.ContentType;
import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CreatePostDTO {
    private String title;

    private String content;

    private ContentType contentType;
    private PostStatus status;
    private PrivacyLevel privacy;

    private String serieName;

    private List<String> categories;
    private List<String> imageUrls;
}
