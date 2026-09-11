package com.nakamahub.backend.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDTO {

    @NotBlank(message = "Falta el token del enlace")
    private String token;

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 carácteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "La contraseña debe ser de al menos 6 caracteres e incluir mayúscular y minúsculas")
    private String newPassword;
}
