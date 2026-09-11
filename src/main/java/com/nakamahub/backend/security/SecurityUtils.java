package com.nakamahub.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Acceso al usuario autenticado sin repetir SecurityContextHolder en cada controlador.
 *
 * Devuelve un Optional vacío en las peticiones anónimas. Antes los controladores
 * llamaban a getName() directamente incluso en endpoints públicos, y en esas rutas
 * Spring devuelve el literal "anonymousUser", que luego se buscaba en base de datos.
 */
public final class SecurityUtils {

    private static final String ANONYMOUS_USER = "anonymousUser";

    private SecurityUtils() {
    }

    public static Optional<String> currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || ANONYMOUS_USER.equals(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    /** Para endpoints que la configuración de seguridad ya obliga a estar autenticados. */
    public static String requireCurrentUsername() {
        return currentUsername().orElseThrow(() -> new IllegalStateException(
                "Se esperaba un usuario autenticado en un endpoint protegido"));
    }
}
