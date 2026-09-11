package com.nakamahub.backend.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvatarDTO {

    @NotBlank(message = "La URL del avatar es obligatoria")
    @Size(max = 500, message = "La URL del avatar no puede superar los 500 caracteres")
    @Pattern(regexp = "^https://.+", message = "La URL del avatar debe empezar por https://")
    private String avatarUrl;
}
