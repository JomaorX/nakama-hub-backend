package com.nakamahub.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Token de refresco, opaco y de un solo uso.
 *
 * Se guarda el hash y no el valor, por el mismo motivo que con las contraseñas: si
 * alguien lee la tabla no puede suplantar a nadie. Cada uso rota el token, así que
 * si uno ya gastado vuelve a aparecer es señal de robo y se revoca toda la familia.
 */
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Null mientras el token sigue vivo. */
    private Instant revokedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RefreshToken token)) {
            return false;
        }
        return id != null && id.equals(token.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
