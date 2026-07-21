package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.MilestoneEventRescheduleService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionScheduleAdjustServiceTest {

    @Mock private HackathonRepository hackathonRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private EventRepository eventRepository;
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private MilestoneEventRescheduleService milestoneEventRescheduleService;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;
    @Mock private NotificationService notificationService;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @Mock private UserRepository userRepository;

    @InjectMocks private CompetitionScheduleAdjustService service;

    @Test
    void apply_shiftsPrelimFinalAndMarksAdjusted() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder()
                .id(9)
                .name("SEAL")
                .registrationEnd(regEnd)
                .eventStart(LocalDate.of(2026, 8, 10))
                .eventEnd(LocalDate.of(2026, 8, 12))
                .build();
        LocalDateTime newExam = LocalDateTime.of(2026, 7, 23, 8, 0);
        Round prelim = Round.builder()
                .id(3).isFinal(false).isActive(false)
                .examAt(LocalDateTime.of(2026, 8, 10, 8, 0))
                .codingDurationHours(7).hackathon(h).build();
        Round finalR = Round.builder()
                .id(4).isFinal(true).isActive(false)
                .examAt(LocalDateTime.of(2026, 8, 10, 17, 0))
                .codingDurationHours(2).hackathon(h).build();

        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findPreliminaryLikeByHackathonId(9)).thenReturn(List.of(prelim));
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(9)).thenReturn(Optional.of(finalR));
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(9)).thenReturn(List.of(prelim, finalR));
        when(hackathonRepository.findById(9)).thenReturn(Optional.of(h));
        when(eventRepository.findByHackathonIdAndType(eq(9), any())).thenReturn(List.of());
        when(milestoneEventRescheduleService.setWorkshopKickoffTimes(any(), any(), any(), any(), any())).thenReturn(2);
        when(milestoneEventRescheduleService.setAwardsTimes(any(), any(), any())).thenReturn(1);
        when(teamRepository.findByHackathon_IdAndStatus(eq(9), any())).thenReturn(List.of());
        when(mentorTeamAssignmentRepository.findByHackathon_Id(9)).thenReturn(List.of());
        when(userRepository.findByRoleAndStatus(any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        Map<String, Object> meta = service.apply(h, newExam, true);

        assertThat(meta.get("scheduleAdjusted")).isEqualTo(true);
        assertThat(h.getScheduleAdjustedAt()).isNotNull();
        assertThat(prelim.getExamAt()).isEqualTo(newExam);
        assertThat(finalR.getExamAt())
                .isEqualTo(RoundScheduleSeedUtil.maxFinalExamAt(newExam, 7));
        verify(milestoneEventRescheduleService).setWorkshopKickoffTimes(any(), any(), any(), any(), any());
        verify(presentationSlotCascadeService).rescheduleForRound(3);
        verify(presentationSlotCascadeService).rescheduleForRound(4);
    }

    @Test
    void apply_rejectsSecondTime() {
        Hackathon h = Hackathon.builder()
                .id(9)
                .registrationEnd(LocalDate.of(2026, 7, 20))
                .scheduleAdjustedAt(LocalDateTime.now().minusDays(1))
                .build();
        assertThatThrownBy(() -> service.apply(h, LocalDateTime.of(2026, 7, 25, 8, 0), true))
                .hasMessageContaining("1 lần");
    }
}
