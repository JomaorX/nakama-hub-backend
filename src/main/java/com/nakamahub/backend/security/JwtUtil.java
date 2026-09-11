package com.nakamahub.backend.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** HMAC-SHA256 pierde garantías si la clave es más corta que su propia salida. */
    private static final int MIN_SECRET_BYTES = 32;

    public static final String ROLE_CLAIM = "role";

    private final String secret;
    private final long expirationMillis;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expirationMillis) {
        this.secret = secret;
        this.expirationMillis = expirationMillis;
    }

    /**
     * Falla el arranque si el secreto es débil. Es preferible no levantar la aplicación
     * a servirla con tokens que cualquiera puede falsificar por fuerza bruta.
     */
    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret no está configurado");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " bytes, tiene " + length
                            + ". Genera uno con: openssl rand -base64 48");
        }
        if (expirationMillis <= 0) {
            throw new IllegalStateException("jwt.expiration debe ser mayor que cero");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(username)
                .withClaim(ROLE_CLAIM, role)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(expirationMillis)))
                .sign(algorithm);
    }

    /**
     * Verifica firma y caducidad, y devuelve el token decodificado una sola vez.
     * Antes había un método por cada claim y cada uno volvía a verificar el token,
     * así que se hacía el doble de trabajo criptográfico en cada petición.
     */
    public Optional<DecodedJWT> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException ex) {
            log.debug("Token JWT rechazado: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
