package com.nakamahub.backend.dtos.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePrivacyDTO {
    @NotBlank(message = "La privacidad es obligatoria")
    private String privacy;
}
