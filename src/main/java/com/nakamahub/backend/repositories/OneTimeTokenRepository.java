package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.OneTimeToken;
import com.nakamahub.backend.models.TokenPurpose;
import com.nakamahub.backend.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /**
     * Invalida los token anteriores del mismo tipo antes de emitir uno nuevo, para
     * que pedir el enlace dos veces no deje dos enlaces válidos circulando.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OneTimeToken t set t.usedAt = :now "
            + "where t.user = :user and t.purpose = :purpose and t.usedAt is null")
    int invalidatePrevious(@Param("user") User user,
                           @Param("purpose") TokenPurpose purpose,
                           @Param("now") Instant now);

    @Modifying
    @Query("delete from OneTimeToken t where t.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
