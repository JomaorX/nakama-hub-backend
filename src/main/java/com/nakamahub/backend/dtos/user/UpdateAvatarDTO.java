package com.nakamahub.backend.dtos.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvatarDTO {
    @NotBlank(message = "La privacidad es obligatoria")
    private String avatarUrl;
}
