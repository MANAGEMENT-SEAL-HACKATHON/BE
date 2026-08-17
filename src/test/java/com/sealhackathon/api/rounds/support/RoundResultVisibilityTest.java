package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoundResultVisibilityTest {

    @Test
    void prelim_requiresPublished() {
        Round round = Round.builder().isFinal(false).isPublished(false).build();
        assertThat(RoundResultVisibility.visibleToParticipants(round, hackathon(HackathonStatus.ONGOING))).isFalse();
        assertThat(RoundResultVisibility.visibleToPublic(round, hackathon(HackathonStatus.ONGOING))).isFalse();

        round.setIsPublished(true);
        assertThat(RoundResultVisibility.visibleToParticipants(round, hackathon(HackathonStatus.ONGOING))).isTrue();
        assertThat(RoundResultVisibility.visibleToPublic(round, hackathon(HackathonStatus.ONGOING))).isTrue();
    }

    @Test
    void final_participants_fromPendingConfirmWhenLocked() {
        Round round = Round.builder().isFinal(true).isPublished(false).scoringLocked(true).build();
        assertThat(RoundResultVisibility.visibleToParticipants(round, hackathon(HackathonStatus.PENDING_CONFIRM))).isTrue();
        assertThat(RoundResultVisibility.visibleToParticipants(round, hackathon(HackathonStatus.FINISHED))).isTrue();
        assertThat(RoundResultVisibility.visibleToParticipants(round, hackathon(HackathonStatus.ONGOING))).isFalse();
    }

    @Test
    void final_public_onlyWhenFinished() {
        Round round = Round.builder().isFinal(true).isPublished(false).scoringLocked(true).build();
        assertThat(RoundResultVisibility.visibleToPublic(round, hackathon(HackathonStatus.PENDING_CONFIRM))).isFalse();
        assertThat(RoundResultVisibility.visibleToPublic(round, hackathon(HackathonStatus.FINISHED))).isTrue();
    }

    @Test
    void final_notVisibleWhenNotLocked() {
        Round round = Round.builder().isFinal(true).isPublished(false).scoringLocked(false).build();
        assertThat(RoundResultVisibility.visibleToParticipants(round, hackathon(HackathonStatus.PENDING_CONFIRM))).isFalse();
    }

    private static Hackathon hackathon(HackathonStatus status) {
        return Hackathon.builder().status(status).build();
    }
}
