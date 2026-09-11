package com.nakamahub.backend.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LoginResponseDTO {

    private Long id;
    private String username;
    private String email;

    /** JWT de vida corta que se manda en la cabecera Authorization. */
    private String accessToken;

    /** Token opaco de vida larga, de un solo uso, para pedir un accessToken nuevo. */
    private String refreshToken;

    /** Segundos de validez del accessToken, para que el cliente sepa cuándo refrescar. */
    private long expiresIn;
}
