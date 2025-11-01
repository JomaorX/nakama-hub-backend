package com.nakamahub.backend.dtos;

import lombok.Data;

@Data
public class LoginResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String token;
}
