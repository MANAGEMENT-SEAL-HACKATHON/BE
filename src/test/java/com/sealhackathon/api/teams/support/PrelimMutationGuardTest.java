package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrelimMutationGuardTest {

    private final PrelimMutationGuard guard = new PrelimMutationGuard();

    @Test
    void allowsParticipating() {
        TeamRoundTrack trt = TeamRoundTrack.builder()
                .team(Team.builder().id(1).build())
                .participationStatus(ParticipationStatus.PARTICIPATING)
                .build();
        assertThatCode(() -> guard.assertPrelimMutable(trt)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAdvancedWith403() {
        TeamRoundTrack trt = TeamRoundTrack.builder()
                .team(Team.builder().id(7).build())
                .participationStatus(ParticipationStatus.ADVANCED)
                .build();
        assertThatThrownBy(() -> guard.assertPrelimMutable(trt))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException auth = (AuthException) ex;
                    assertThat(auth.getCode()).isEqualTo(ErrorCode.PRELIM_NOT_MUTABLE);
                    assertThat(auth.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void rejectsEliminatedWith403() {
        TeamRoundTrack trt = TeamRoundTrack.builder()
                .team(Team.builder().id(8).build())
                .participationStatus(ParticipationStatus.ELIMINATED)
                .build();
        assertThatThrownBy(() -> guard.assertPrelimMutable(trt))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException auth = (AuthException) ex;
                    assertThat(auth.getCode()).isEqualTo(ErrorCode.PRELIM_NOT_MUTABLE);
                    assertThat(auth.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
