package com.nakamahub.backend.dtos.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBioDTO {
    @Size(min = 4, max = 120, message = "La biografía debe tener entre 4 y 120 caracteres")
    private String bio;
}
