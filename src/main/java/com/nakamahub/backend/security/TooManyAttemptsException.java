package com.nakamahub.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/** Se han agotado los intentos permitidos. Lleva cuánto falta para poder reintentar. */
public class TooManyAttemptsException extends ResponseStatusException {

    private final long retryAfterSeconds;

    public TooManyAttemptsException(Duration retryAfter, String reason) {
        super(HttpStatus.TOO_MANY_REQUESTS, reason);
        this.retryAfterSeconds = Math.max(1, retryAfter.toSeconds());
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
