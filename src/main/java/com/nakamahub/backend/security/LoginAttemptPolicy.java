package com.nakamahub.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Reglas concretas de limitación para autenticación y registro.
 *
 * Se cuenta por cuenta y por dirección, porque cada uno cubre un ataque distinto:
 * el primero frena probar mil contraseñas contra un usuario, el segundo frena
 * probar una contraseña frecuente contra mil usuarios.
 */
@Component
public class LoginAttemptPolicy {

    static final Duration WINDOW = Duration.ofMinutes(15);

    static final int MAX_PER_ACCOUNT = 5;
    static final int MAX_PER_ADDRESS = 25;
    static final int MAX_SIGNUPS_PER_ADDRESS = 10;

    private final AttemptLimiter limiter;

    public LoginAttemptPolicy(AttemptLimiter limiter) {
        this.limiter = limiter;
    }

    public void checkLogin(String identifier, HttpServletRequest request) {
        limiter.check(accountKey(identifier), MAX_PER_ACCOUNT, WINDOW);
        limiter.check(addressKey(request), MAX_PER_ADDRESS, WINDOW);
    }

    public void recordLoginFailure(String identifier, HttpServletRequest request) {
        limiter.recordFailure(accountKey(identifier), WINDOW);
        limiter.recordFailure(addressKey(request), WINDOW);
    }

    public void recordLoginSuccess(String identifier, HttpServletRequest request) {
        limiter.reset(accountKey(identifier));
        limiter.reset(addressKey(request));
    }

    public void checkSignup(HttpServletRequest request) {
        limiter.check(signupKey(request), MAX_SIGNUPS_PER_ADDRESS, WINDOW);
    }

    public void recordSignup(HttpServletRequest request) {
        limiter.recordFailure(signupKey(request), WINDOW);
    }

    /** En minúsculas para que variar las mayúsculas no cuente como otra cuenta. */
    private String accountKey(String identifier) {
        return "login:cuenta:" + (identifier == null ? "" : identifier.toLowerCase(Locale.ROOT));
    }

    private String addressKey(HttpServletRequest request) {
        return "login:ip:" + request.getRemoteAddr();
    }

    private String signupKey(HttpServletRequest request) {
        return "registro:ip:" + request.getRemoteAddr();
    }
}
