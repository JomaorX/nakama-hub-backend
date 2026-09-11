package com.nakamahub.backend.dtos.post;

import com.nakamahub.backend.models.ContentType;
import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    // Este DTO no tenía ninguna restricción y el controlador tampoco usaba @Valid,
    // así que se aceptaban títulos vacíos, contenido sin límite y categorías nulas,
    // que además rompían el servicio con un error 500.

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 4, max = 150, message = "El título debe tener entre 4 y 150 caracteres")
    private String title;

    @NotBlank(message = "El contenido es obligatorio")
    @Size(min = 10, max = 20000, message = "El contenido debe tener entre 10 y 20000 caracteres")
    private String content;

    @NotNull(message = "El tipo de contenido es obligatorio")
    private ContentType contentType;

    private PostStatus status;

    private PrivacyLevel privacy;

    @Size(max = 150, message = "El nombre de la serie no puede superar los 150 caracteres")
    private String serieName;

    @NotEmpty(message = "Debes indicar al menos una categoría")
    @Size(max = 5, message = "Un post no puede tener más de 5 categorías")
    private List<@NotBlank(message = "El nombre de la categoría no puede estar vacío") String> categories;

    @Size(max = 10, message = "Un post no puede tener más de 10 imágenes")
    private List<@NotBlank(message = "La URL de la imagen no puede estar vacía") String> imageUrls;
}
