package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HackathonRegistrationSupportTest {

    @Test
    void canRunLotteryRequiresRegistrationEndedAndAllActiveLocked() {
        Hackathon hackathon = Hackathon.builder()
                .registrationEnd(LocalDate.now().minusDays(1))
                .build();
        List<Team> teams = List.of(
                Team.builder().status(TeamStatus.ACTIVE).isLocked(true).build(),
                Team.builder().status(TeamStatus.PENDING).isLocked(false).build());

        assertThat(HackathonRegistrationSupport.canRunLottery(hackathon, teams)).isTrue();
    }

    @Test
    void canRunLotteryBlocksWhenActiveTeamNotLocked() {
        Hackathon hackathon = Hackathon.builder()
                .registrationEnd(LocalDate.now().minusDays(1))
                .build();
        List<Team> teams = List.of(
                Team.builder().status(TeamStatus.ACTIVE).isLocked(false).build());

        assertThat(HackathonRegistrationSupport.canRunLottery(hackathon, teams)).isFalse();
    }
}
