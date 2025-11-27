package com.nakamahub.backend.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUsernameDTO {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 22, message = "El nombre de usuario debe tener entre 4 y 22 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "El nombre de usuario solo puede contener letras, números y guiones bajos"
    )
    private String username;
}
