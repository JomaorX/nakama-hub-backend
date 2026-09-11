package com.nakamahub.backend.dtos.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateCommentDTO {

    @NotBlank(message = "El comentario no puede estar en blanco")
    @Size(min = 6, max = 1000, message = "El comentario debe tener entre 6 y 1000 carácteres")
    private String content;
}
