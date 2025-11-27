package com.nakamahub.backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailDTO {
    @NotBlank(message = "El Email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;
}
