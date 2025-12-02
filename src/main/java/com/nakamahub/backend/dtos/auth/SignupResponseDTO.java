package com.nakamahub.backend.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignupResponseDTO {

    @Schema(description = "ID único del usuario", example = "1")
    private Long id;

    @Schema(description = "Correo electrónico registrado", example = "nakama@example.com")
    private String email;

    @Schema(description = "Nombre de usuario registrado", example = "nakama123")
    private String username;

}
