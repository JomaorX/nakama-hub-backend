package com.nakamahub.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Token de un solo uso que viaja por correo.
 *
 * Como con los tokens de refresco, se guarda el hash y no el valor: quien lea la
 * tabla no debe poder restablecer la contraseña de nadie. Y como allí, basta un
 * hash rápido sin sal, porque el original son bytes aleatorios y no hay nada que
 * adivinar por fuerza bruta.
 */
@Entity
@Table(name = "one_time_tokens")
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class OneTimeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private TokenPurpose purpose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Null hasta que se canjea. Un token gastado no vuelve a servir. */
    private Instant usedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OneTimeToken token)) {
            return false;
        }
        return id != null && id.equals(token.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
