package com.nakamahub.backend.models;

public enum TokenPurpose {
    /** Confirmar que la dirección de correo es de quien se registró. */
    VERIFICACION_EMAIL,
    /** Restablecer la contraseña de quien no puede entrar. */
    RESTABLECER_PASSWORD
}
