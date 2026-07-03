package com.sealhackathon.api.rounds.job;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundDeadlineReminderSchedulerTest {

    @Mock private RoundRepository roundRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private RoundDeadlineReminderScheduler scheduler;

    @BeforeEach
    void setLeadHours() {
        ReflectionTestUtils.setField(scheduler, "leadHours", 24);
    }

    @Test
    void runRoundDeadlineReminders_dueRound_notifiesStudentsAndJudgesAndMarksSent() {
        LocalDateTime now = LocalDateTime.now();
        Round round = Round.builder()
                .id(9)
                .name("Sơ loại")
                .submissionDeadline(now.plusHours(12))
                .isActive(true)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        when(roundRepository.findActiveWithUpcomingDeadlineWithoutReminder(any(), any()))
                .thenReturn(List.of(round));
        when(teamRepository.findByHackathon_IdAndStatus(1, TeamStatus.ACTIVE))
                .thenReturn(List.of(Team.builder().id(1).build()));
        when(teamMemberRepository.findByTeam_Id(1)).thenReturn(List.of(
                TeamMember.builder().user(User.builder().id(11).build()).status(TeamMemberStatus.ACCEPTED).build()));
        when(judgeAssignmentRepository.findByRoundId(9)).thenReturn(List.of(
                JudgeAssignment.builder().judge(User.builder().id(21).build()).build()));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(9)).thenReturn(List.of());

        scheduler.runRoundDeadlineReminders();

        verify(notificationService).sendBatch(
                anyList(), eq("SUBMISSION_DEADLINE_REMINDER"), anyString(), anyString(), eq("rounds"), eq(9));
        verify(notificationService).sendBatch(
                anyList(), eq("JUDGE_SCORING_REMINDER"), anyString(), anyString(), eq("rounds"), eq(9));
        verify(roundRepository).save(round);
        Assertions.assertNotNull(round.getDeadlineReminderSentAt());
    }

    @Test
    void runRoundDeadlineReminders_noDueRounds_skips() {
        when(roundRepository.findActiveWithUpcomingDeadlineWithoutReminder(any(), any()))
                .thenReturn(List.of());

        scheduler.runRoundDeadlineReminders();

        verify(notificationService, never()).sendBatch(any(), any(), any(), any(), any(), any());
        verify(roundRepository, never()).save(any());
    }
}
