package com.nakamahub.backend.services;

import com.nakamahub.backend.models.OneTimeToken;
import com.nakamahub.backend.models.TokenPurpose;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.OneTimeTokenRepository;
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
import java.util.Map;

/** Emisión y canje de los token de un solo uso que viajan por correo. */
@Service
@Transactional
public class OneTimeTokenService {

    private static final int TOKEN_BYTES = 32;

    /**
     * El de restablecimiento dura poco porque da acceso a la cuenta. El de
     * verificación puede durar más: quien lo recibe ya es el dueño del buzón.
     */
    private static final Map<TokenPurpose, Duration> LIFETIMES = Map.of(
            TokenPurpose.RESTABLECER_PASSWORD, Duration.ofHours(1),
            TokenPurpose.VERIFICACION_EMAIL, Duration.ofDays(2));

    private final OneTimeTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OneTimeTokenService(OneTimeTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /** @return el valor en claro, que solo se ve aquí y en el correo. */
    public String issue(User user, TokenPurpose purpose) {
        Instant now = Instant.now();
        tokenRepository.deleteExpiredBefore(now.minus(Duration.ofDays(30)));
        tokenRepository.invalidatePrevious(user, purpose, now);

        String rawToken = randomToken();

        OneTimeToken token = new OneTimeToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plus(LIFETIMES.get(purpose)));
        tokenRepository.save(token);

        return rawToken;
    }

    /** Canjea el token y lo marca como gastado. Devuelve el usuario al que pertenece. */
    public User consume(String rawToken, TokenPurpose purpose) {
        OneTimeToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .filter(candidate -> candidate.getPurpose() == purpose)
                .filter(OneTimeToken::isUsable)
                // Mismo mensaje para un token inexistente, caducado o ya usado: el
                // detalle no ayuda a quien lo recibió y sí a quien lo está probando.
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El enlace no es válido o ha caducado"));

        token.setUsedAt(Instant.now());
        return token.getUser();
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 debería estar disponible en cualquier JVM", ex);
        }
    }
}
