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
public class ChangePasswordDTO {

    /** Se exige la actual para que un token robado no baste para secuestrar la cuenta. */
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 carácteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "La contraseña debe ser de al menos 6 caracteres e incluir mayúscular y minúsculas")
    private String newPassword;
}
