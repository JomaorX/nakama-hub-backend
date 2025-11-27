package com.nakamahub.backend.dtos.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignupResponseDTO {

    private Long id;
    private String email;
    private String username;

}
