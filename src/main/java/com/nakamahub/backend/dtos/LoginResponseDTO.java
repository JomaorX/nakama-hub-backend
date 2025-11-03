package com.nakamahub.backend.dtos;

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
    private String token;
}
