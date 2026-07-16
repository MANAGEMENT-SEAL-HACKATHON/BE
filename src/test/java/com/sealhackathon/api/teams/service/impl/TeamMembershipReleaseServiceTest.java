package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamMembershipReleaseServiceTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private HackathonRegistrationRepository hackathonRegistrationRepository;
    @Mock private HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private TeamMembershipReleaseServiceImpl service;

    @Test
    void releaseMembers_setsAcceptedToLeftAndPendingToRejected() {
        Hackathon hackathon = Hackathon.builder().id(1).build();
        Team team = Team.builder().id(10).hackathon(hackathon).teamName("Alpha").build();
        User acceptedUser = User.builder().id(100).fullName("SV A").build();
        User pendingUser = User.builder().id(101).fullName("SV B").build();

        TeamMember accepted = TeamMember.builder()
                .id(new TeamMemberId(10, 100))
                .team(team)
                .user(acceptedUser)
                .roleInTeam(TeamMemberRole.MEMBER)
                .status(TeamMemberStatus.ACCEPTED)
                .build();
        TeamMember pending = TeamMember.builder()
                .id(new TeamMemberId(10, 101))
                .team(team)
                .user(pendingUser)
                .roleInTeam(TeamMemberRole.MEMBER)
                .status(TeamMemberStatus.PENDING)
                .build();

        when(teamMemberRepository.findByTeam_Id(10)).thenReturn(List.of(accepted, pending));

        service.releaseMembers(team, "Không đủ điều kiện", false);

        assertEquals(TeamMemberStatus.LEFT, accepted.getStatus());
        assertEquals(TeamMemberStatus.REJECTED, pending.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<User>> usersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).sendBatch(
                usersCaptor.capture(),
                eq("TEAM_RELEASED"),
                any(),
                any(),
                eq("teams"),
                eq(10));
        assertEquals(1, usersCaptor.getValue().size());
        assertEquals(100, usersCaptor.getValue().get(0).getId());
    }
}
