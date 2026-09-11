package com.nakamahub.backend.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Canje de un token que llegó por correo, como el de verificación de la dirección. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestDTO {

    @NotBlank(message = "Falta el token del enlace")
    private String token;
}
