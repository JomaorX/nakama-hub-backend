package com.nakamahub.backend.dtos.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CommentResponseDTO {
    private Long id;
    private String content;
    private Long postId;
    private String authorUsername;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Ver la explicación en PostResponseDTO.edited. */
    private boolean edited;

    /** Cuántas respuestas cuelgan de este comentario, para no tener que pedirlas por si acaso. */
    private long replyCount;

    /** Si quien consulta es el autor, para decidir si ofrecer editar y borrar. */
    private boolean own;
}
