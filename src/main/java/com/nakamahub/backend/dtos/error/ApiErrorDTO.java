package com.nakamahub.backend.dtos.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo único de respuesta para cualquier error de la API.
 * Mantener un solo formato simplifica el cliente: siempre hay status y message,
 * y fieldErrors solo aparece cuando falla la validación de un DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiErrorDTO of(int status, String error, String message, String path) {
        return new ApiErrorDTO(Instant.now(), status, error, message, path, null);
    }

    public static ApiErrorDTO ofValidation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiErrorDTO(Instant.now(), status, "Bad Request", message, path, fieldErrors);
    }
}
