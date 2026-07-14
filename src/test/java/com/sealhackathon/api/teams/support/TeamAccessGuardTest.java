package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 4 — document IDOR deny on team journey/details (foreign student).
 */
@ExtendWith(MockitoExtension.class)
class TeamAccessGuardTest {

    @Mock
    private CurrentUserAccessor currentUserAccessor;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;

    @InjectMocks
    private TeamAccessGuard guard;

    @Test
    void assertCanViewTeamDetails_coordinator_bypasses() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(1).role(UserRole.COORDINATOR).build());
        guard.assertCanViewTeamDetails(99);
        verify(teamMemberRepository, never()).existsByUser_IdAndTeam_IdAndMemberStatusIn(anyInt(), anyInt(), any());
    }

    @Test
    void assertCanViewTeamDetails_foreignStudent_throwsForbidden() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(42).role(UserRole.STUDENT).build());
        when(teamMemberRepository.existsByUser_IdAndTeam_IdAndMemberStatusIn(eq(42), eq(99), any()))
                .thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> guard.assertCanViewTeamDetails(99));
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }
}
