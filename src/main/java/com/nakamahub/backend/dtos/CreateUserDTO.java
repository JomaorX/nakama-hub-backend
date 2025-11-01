package com.nakamahub.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDTO {

    @NotBlank(message = "El Email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 22, message = "El nombre de usuario debe tener entre 4 y 22 carácteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 carácteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).*$",
        message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")

    private String password;
}
