package com.sealhackathon.api.presentation.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationControllerAuthTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @InjectMocks
    private PresentationControllerGuard guard;

    @Test
    void headJudgeCanControlTrackByDefault() {
        Track track = Track.builder().id(10).build();
        Round round = Round.builder().id(5).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(7).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(judgeAssignmentRepository.findByTrackId(10)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .assignmentType(JudgeAssignmentType.HEAD)
                        .judge(User.builder().id(7).build())
                        .build()));
        assertThatCode(() -> guard.requireControllerForTrack(10, track, round)).doesNotThrowAnyException();
    }

    @Test
    void coordinatorAlwaysAllowed() {
        Track track = Track.builder().id(10).build();
        Round round = Round.builder().id(5).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(1).role(UserRole.COORDINATOR).build());
        assertThatCode(() -> guard.requireControllerForTrack(10, track, round)).doesNotThrowAnyException();
    }

    @Test
    void nonControllerJudgeForbidden() {
        Track track = Track.builder().id(10).build();
        Round round = Round.builder().id(5).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(8).role(UserRole.JUDGE).build());
        when(judgeAssignmentRepository.findByTrackId(10)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .assignmentType(JudgeAssignmentType.HEAD)
                        .judge(User.builder().id(7).build())
                        .build()));
        assertThatThrownBy(() -> guard.requireControllerForTrack(10, track, round))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void roundControllerGrantAllowsFinal() {
        User controller = User.builder().id(12).build();
        Round round = Round.builder().id(20).controllerJudge(controller).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(12).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(12);
        assertThatCode(() -> guard.requireControllerForRound(20, round)).doesNotThrowAnyException();
    }

    @Test
    void earliestAssignedJudgeIsDefaultForFinalRound() {
        Round round = Round.builder().id(20).build();
        User laterJudge = User.builder().id(9).isDeptHead(true).build();
        User earlierJudge = User.builder().id(11).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(11).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(judgeAssignmentRepository.findByRoundId(20)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .judge(earlierJudge)
                        .assignedAt(LocalDateTime.now().minusDays(1))
                        .build(),
                JudgeAssignment.builder()
                        .judge(laterJudge)
                        .assignedAt(LocalDateTime.now())
                        .build()));
        assertThatCode(() -> guard.requireControllerForRound(20, round)).doesNotThrowAnyException();
    }

    @Test
    void deptHeadFlagNoLongerOverridesEarliestAssignee() {
        Round round = Round.builder().id(21).build();
        User deptHead = User.builder().id(9).isDeptHead(true).build();
        User earliest = User.builder().id(11).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(9).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(9);
        when(judgeAssignmentRepository.findByRoundId(21)).thenReturn(List.of(
                JudgeAssignment.builder()
                        .judge(earliest)
                        .assignedAt(LocalDateTime.now().minusDays(2))
                        .build(),
                JudgeAssignment.builder()
                        .judge(deptHead)
                        .assignedAt(LocalDateTime.now())
                        .build()));
        assertThatThrownBy(() -> guard.requireControllerForRound(21, round))
                .isInstanceOf(AuthException.class);
    }
}
