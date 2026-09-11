package com.nakamahub.backend.dtos.user;

import com.nakamahub.backend.models.ProfilePrivacy;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePrivacyDTO {

    /**
     * Tipado como enum a propósito. Antes llegaba como String y el servicio hacía
     * valueOf, de modo que un valor no contemplado terminaba en un error 500 en vez
     * de en un 400. Ahora lo rechaza Jackson al deserializar.
     */
    @NotNull(message = "La privacidad es obligatoria")
    private ProfilePrivacy privacy;
}
