package com.sealhackathon.api.users.repository;

import com.sealhackathon.api.users.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {

    Optional<UserSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    Optional<UserSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE UserSession s
            SET s.revokedAt = :revokedAt
            WHERE s.user.id = :userId AND s.revokedAt IS NULL
            """)
    int revokeAllActiveByUserId(@Param("userId") Integer userId,
                                @Param("revokedAt") LocalDateTime revokedAt);
}
