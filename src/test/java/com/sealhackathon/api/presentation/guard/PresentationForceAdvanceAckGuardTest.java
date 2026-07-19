package com.sealhackathon.api.presentation.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationForceAdvanceAckGuardTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private PresentationControllerGuard controllerGuard;
    @Mock private TrackRepository trackRepository;

    @InjectMocks private PresentationForceAdvanceAckGuard guard;

    @Test
    void coordinator_canAcknowledge() {
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(1).role(UserRole.COORDINATOR).build());

        assertThat(guard.resolveAcknowledge(true, 7, Round.builder().id(1).build())).isTrue();
    }

    @Test
    void trackControllerJudge_canAcknowledge() {
        Track track = Track.builder().id(7).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(5).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(5);
        when(trackRepository.findById(7)).thenReturn(Optional.of(track));
        when(controllerGuard.resolveTrackControllerId(track)).thenReturn(5);

        assertThat(guard.resolveAcknowledge(true, 7, Round.builder().id(1).build())).isTrue();
    }

    @Test
    void roundControllerJudge_canAcknowledge() {
        Round round = Round.builder().id(3).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(9).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(9);
        when(controllerGuard.resolveRoundControllerId(round)).thenReturn(9);

        assertThat(guard.resolveAcknowledge(true, null, round)).isTrue();
    }

    @Test
    void nonControllerJudge_cannotAcknowledge() {
        Track track = Track.builder().id(7).build();
        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(5).role(UserRole.JUDGE).build());
        when(currentUserAccessor.currentUserId()).thenReturn(5);
        when(trackRepository.findById(7)).thenReturn(Optional.of(track));
        when(controllerGuard.resolveTrackControllerId(track)).thenReturn(99);

        assertThatThrownBy(() -> guard.resolveAcknowledge(true, 7, Round.builder().id(1).build()))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void requestedFalse_returnsFalseWithoutRoleCheck() {
        assertThat(guard.resolveAcknowledge(false, 7, Round.builder().id(1).build())).isFalse();
    }
}
