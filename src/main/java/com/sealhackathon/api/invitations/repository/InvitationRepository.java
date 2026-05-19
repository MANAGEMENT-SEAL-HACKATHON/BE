package com.sealhackathon.api.invitations.repository;

import com.sealhackathon.api.invitations.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Integer> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByEmailAndAcceptedAtIsNull(String email);

    List<Invitation> findByEmail(String email);
}
