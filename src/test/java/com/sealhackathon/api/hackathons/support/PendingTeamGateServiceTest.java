package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingTeamGateServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private PendingTeamGateService pendingTeamGateService;

    @Test
    void snapshot_classifiesAwaitingGraceAndBlocked() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 17, 10, 0);
        Team awaiting = Team.builder()
                .id(1)
                .status(TeamStatus.PENDING)
                .formationSubmittedAt(now.minusHours(1))
                .build();
        Team grace = Team.builder()
                .id(2)
                .status(TeamStatus.PENDING)
                .formationGraceDeadlineAt(now.plusHours(20))
                .build();
        Team blocked = Team.builder()
                .id(3)
                .status(TeamStatus.PENDING)
                .formationGraceDeadlineAt(now.minusHours(1))
                .build();
        when(teamRepository.findByHackathon_IdAndStatus(42, TeamStatus.PENDING))
                .thenReturn(List.of(awaiting, grace, blocked));

        PendingTeamGateSnapshot snap = pendingTeamGateService.snapshot(42, now);

        assertThat(snap.awaitingApprovalCount()).isEqualTo(1);
        assertThat(snap.graceCount()).isEqualTo(1);
        assertThat(snap.blockedOtherCount()).isEqualTo(1);
        assertThat(snap.total()).isEqualTo(3);
        assertThat(snap.earliestGraceDeadlineAt()).isEqualTo(grace.getFormationGraceDeadlineAt());
        assertThat(snap.message()).contains("đã xác nhận").contains("24h").contains("xem lại");
        assertThat(snap.details())
                .containsEntry("pendingTotal", 3)
                .containsEntry("awaitingApprovalCount", 1)
                .containsEntry("graceCount", 1)
                .containsEntry("blockedOtherCount", 1);
    }

    @Test
    void assertNoPendingTeams_throwsWhenPendingRemain() {
        when(teamRepository.findByHackathon_IdAndStatus(7, TeamStatus.PENDING))
                .thenReturn(List.of(Team.builder()
                        .id(1)
                        .status(TeamStatus.PENDING)
                        .formationSubmittedAt(LocalDateTime.now())
                        .build()));

        assertThatThrownBy(() -> pendingTeamGateService.assertNoPendingTeams(7))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo(ErrorCode.TEAMS_PENDING_APPROVAL);
                    assertThat(bre.getDetails()).containsEntry("awaitingApprovalCount", 1);
                });
    }

    @Test
    void assertNoPendingTeams_passesWhenEmpty() {
        when(teamRepository.findByHackathon_IdAndStatus(7, TeamStatus.PENDING))
                .thenReturn(List.of());
        pendingTeamGateService.assertNoPendingTeams(7);
    }
}
