package com.sealhackathon.api.notifications.support;

import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonStakeholderRecipientsTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private HackathonStakeholderRecipients recipients;

    @Test
    void collect_unionsMentorModelsAndDedupesByUserId() {
        User student = user(1, "Student");
        User mentorBoth = user(2, "Mentor Both");
        User mentorTrackOnly = user(3, "Mentor Track");
        User judge = user(4, "Judge");
        User coordinator = user(5, "Coord");

        Team team = Team.builder().id(10).build();
        when(teamRepository.findByHackathon_IdAndStatus(7, TeamStatus.ACTIVE)).thenReturn(List.of(team));
        when(teamMemberRepository.findByTeam_IdIn(List.of(10))).thenReturn(List.of(
                TeamMember.builder().user(student).status(TeamMemberStatus.ACCEPTED).build(),
                TeamMember.builder().user(mentorBoth).status(TeamMemberStatus.PENDING).build()));

        when(mentorTeamAssignmentRepository.findByHackathon_Id(7)).thenReturn(List.of(
                MentorTeamAssignment.builder().mentor(mentorBoth).build()));
        when(mentorAssignmentRepository.findByTrack_Round_Hackathon_Id(7)).thenReturn(List.of(
                MentorAssignment.builder().mentor(mentorBoth).build(),
                MentorAssignment.builder().mentor(mentorTrackOnly).build()));

        Round round = Round.builder().id(20).build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(7)).thenReturn(List.of(round));
        when(judgeAssignmentRepository.findByRound_IdIn(List.of(20))).thenReturn(List.of(
                JudgeAssignment.builder().judge(judge).build()));

        Track track = Track.builder().id(30).build();
        when(trackRepository.findByHackathonIdOrderById(7)).thenReturn(List.of(track));
        when(judgeAssignmentRepository.findByTrack_IdIn(List.of(30))).thenReturn(List.of(
                JudgeAssignment.builder().judge(judge).build()));

        when(userRepository.findByRoleAndStatus(
                eq(UserRole.COORDINATOR), eq(UserStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coordinator)));

        List<User> result = recipients.collect(7);

        assertThat(result).extracting(User::getId)
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void collect_returnsEmptyWhenHackathonIdNull() {
        assertThat(recipients.collect(null)).isEmpty();
    }

    private static User user(int id, String name) {
        return User.builder().id(id).fullName(name).email(name.toLowerCase() + "@ex.com").build();
    }
}
