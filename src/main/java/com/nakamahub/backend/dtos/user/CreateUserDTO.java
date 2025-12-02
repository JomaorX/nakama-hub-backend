package com.nakamahub.backend.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDTO {

    @Schema(description = "Correo electrónico del usuario", example = "nakama@example.com")
    @NotBlank(message = "El Email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @Schema(description = "Nombre de usuario único", example = "nakama123")
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 22, message = "El nombre de usuario debe tener entre 4 y 22 carácteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
    message = "El nombre de usuario solo puede contener letras, números y guiones bajos"
    )
    private String username;

    @Schema(description = "Contraseña segura con mayúsculas y minúsculas", example = "Password123")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 carácteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).{6,}$", message = "La contraseña debe ser de al menos 6 caracteres e incluir mayúscular y minúsculas")

    private String password;
}
