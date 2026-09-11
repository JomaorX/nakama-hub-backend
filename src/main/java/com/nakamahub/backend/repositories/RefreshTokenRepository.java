package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.RefreshToken;
import com.nakamahub.backend.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);

    /** Revoca de golpe toda la familia de un usuario: cambio de contraseña, baja o robo detectado. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.user = :user and t.revokedAt is null")
    int revokeAllForUser(@Param("user") User user, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.user = :user and (t.expiresAt < :now or t.revokedAt < :cutoff)")
    void deleteStaleForUser(@Param("user") User user, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
