package com.nakamahub.backend.dtos.post;

import com.nakamahub.backend.models.ContentType;
import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
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

    private PostStatus status;

    private PrivacyLevel privacy;

    private String serieName;

    List<String> categories;

    private String authorUsername;

    private List<String> imageUrls;

    private int viewsCount;

    private int likesCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** Si el autor ha editado el post después de publicarlo. Ver Post.editedAt. */
    private boolean edited;

    /** Si quien consulta ya dio me gusta. False para visitantes anónimos. */
    private boolean likedByMe;

    /** Si quien consulta es el autor, para decidir si ofrecer editar y borrar. */
    private boolean own;
}
