package com.nakamahub.backend.services;

import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Revocación que tiene que sobrevivir a un rollback.
 *
 * Cuando se detecta la reutilización de un token se revoca la familia y acto seguido
 * se lanza un 401. Si la revocación fuese en la misma transacción, la excepción la
 * desharía y el token robado seguiría vivo, que es justo lo contrario de lo que se
 * busca. Va en un bean aparte porque una llamada a un método propio no pasa por el
 * proxy de Spring y la propagación se ignoraría.
 */
@Service
public class RefreshTokenRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenRevoker(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }
}
