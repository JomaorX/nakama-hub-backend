package com.nakamahub.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nakamahub.backend.dtos.error.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Escribe errores en formato ApiErrorDTO desde dentro de la cadena de filtros.
 *
 * Los filtros se ejecutan antes del DispatcherServlet, así que las excepciones que
 * lanzan no pasan por el @RestControllerAdvice: el cliente recibiría un 500 genérico
 * en lugar del código real. Por eso aquí se escribe la respuesta a mano.
 */
@Component
public class HttpErrorResponder {

    private final ObjectMapper objectMapper;

    public HttpErrorResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorDTO.of(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
