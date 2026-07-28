package com.sealhackathon.api.me.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * SEC-AUTH-01 — Student without registration/membership must not read leaderboard (403).
 */
@ExtendWith(MockitoExtension.class)
class StudentAccessGuardParticipatedTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private HackathonRegistrationRepository hackathonRegistrationRepository;

    @InjectMocks private StudentAccessGuard studentAccessGuard;

    @Test
    void assertParticipatedInHackathon_rejectsNonParticipantWith403() {
        when(currentUserAccessor.currentUserId()).thenReturn(99);
        when(hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(7, 99)).thenReturn(false);
        when(teamMemberRepository.existsAcceptedMembershipInHackathon(99, 7)).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class,
                () -> studentAccessGuard.assertParticipatedInHackathon(7));

        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void assertParticipatedInHackathon_allowsRegisteredStudent() {
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(7, 11)).thenReturn(true);

        assertDoesNotThrow(() -> studentAccessGuard.assertParticipatedInHackathon(7));
    }

    @Test
    void assertParticipatedInHackathon_allowsAcceptedTeamMemberWithoutRegistrationRow() {
        when(currentUserAccessor.currentUserId()).thenReturn(22);
        when(hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(7, 22)).thenReturn(false);
        when(teamMemberRepository.existsAcceptedMembershipInHackathon(22, 7)).thenReturn(true);

        assertDoesNotThrow(() -> studentAccessGuard.assertParticipatedInHackathon(7));
    }
}
