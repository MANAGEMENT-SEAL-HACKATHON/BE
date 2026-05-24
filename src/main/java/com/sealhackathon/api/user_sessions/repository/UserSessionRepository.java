package com.sealhackathon.api.user_sessions.repository;

import com.sealhackathon.api.user_sessions.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {

    Optional<UserSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}
