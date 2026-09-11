package com.nakamahub.backend.services;

import com.nakamahub.backend.models.RefreshToken;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final int TOKEN_BYTES = 32;

    /** Cuánto se conservan los tokens ya revocados antes de borrarlos, para poder investigar un robo. */
    private static final Duration REVOKED_RETENTION = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenRevoker refreshTokenRevoker;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration lifetime;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               RefreshTokenRevoker refreshTokenRevoker,
                               @Value("${jwt.refresh-expiration}") long refreshExpirationMillis) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenRevoker = refreshTokenRevoker;
        this.lifetime = Duration.ofMillis(refreshExpirationMillis);
    }

    /** Emite un token nuevo y devuelve su valor en claro, que solo se ve aquí y en la respuesta. */
    public String issue(User user) {
        Instant now = Instant.now();
        refreshTokenRepository.deleteStaleForUser(user, now, now.minus(REVOKED_RETENTION));

        String rawToken = randomToken();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plus(lifetime));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    /**
     * Canjea un token por otro. Rotar en cada uso reduce la ventana de un token robado
     * y, sobre todo, permite detectar el robo: si reaparece uno ya gastado es que hay
     * dos partes usando la misma sesión, así que se revoca la familia entera y ambas
     * tienen que volver a autenticarse.
     */
    public User rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Token de refresco no válido"));

        User user = token.getUser();

        if (token.isRevoked()) {
            log.warn("Reutilización de un token de refresco ya gastado del usuario {}, se revoca la familia",
                    user.getId());
            // En transacción aparte: el 401 de abajo haría rollback de esta revocación.
            refreshTokenRevoker.revokeAllForUser(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token de refresco reutilizado, vuelve a iniciar sesión");
        }

        if (token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de refresco caducado");
        }

        token.setRevokedAt(Instant.now());
        return user;
    }

    /** Cierre de sesión. No falla si el token ya no existe: el resultado buscado es el mismo. */
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isUsable)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    public void revokeAllFor(User user) {
        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 a secas, sin sal ni coste, a diferencia de las contraseñas. Aquí el valor
     * original son 32 bytes aleatorios, así que no hay nada que adivinar por fuerza bruta
     * y el hash tiene que ser rápido porque se calcula en cada refresco.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 debería estar disponible en cualquier JVM", ex);
        }
    }
}
