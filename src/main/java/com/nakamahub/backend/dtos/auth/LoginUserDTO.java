package com.nakamahub.backend.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginUserDTO {

    @Schema(description = "Identificador (puede ser email o username)")
    @NotBlank(message = "El identificador es obligatorio")
    private String identifier;

    @Schema(description = "Contraseña del usuario", example = "Password123")
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
