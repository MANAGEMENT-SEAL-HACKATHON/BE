package com.sealhackathon.api.presentation.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationForceAdvanceAckGuardTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @InjectMocks private PresentationForceAdvanceAckGuard guard;

    @Test
    void coordinator_canAcknowledge() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(1).role(UserRole.COORDINATOR).build());

        assertThat(guard.resolveAcknowledge(true, 7, Round.builder().id(1).build())).isTrue();
    }

    @Test
    void headJudgeOnTrack_canAcknowledge() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(5).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(5);
        when(judgeAssignmentRepository.findByJudgeIdAndTrackId(5, 7)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .assignmentType(JudgeAssignmentType.HEAD)
                        .judge(User.builder().id(5).build())
                        .build()));

        assertThat(guard.resolveAcknowledge(true, 7, Round.builder().id(1).build())).isTrue();
    }

    @Test
    void headJudgeOnFinalRound_canAcknowledge() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(9).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(9);
        when(judgeAssignmentRepository.findByRoundId(3)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .assignmentType(JudgeAssignmentType.HEAD)
                        .judge(User.builder().id(9).build())
                        .build()));

        assertThat(guard.resolveAcknowledge(true, null, Round.builder().id(3).build())).isTrue();
    }

    @Test
    void normalJudge_cannotAcknowledge() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(5).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(5);
        when(judgeAssignmentRepository.findByJudgeIdAndTrackId(5, 7)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .assignmentType(JudgeAssignmentType.NORMAL)
                        .judge(User.builder().id(5).build())
                        .build()));

        assertThatThrownBy(() -> guard.resolveAcknowledge(true, 7, Round.builder().id(1).build()))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void requestedFalse_returnsFalseWithoutRoleCheck() {
        assertThat(guard.resolveAcknowledge(false, 7, Round.builder().id(1).build())).isFalse();
    }
}
