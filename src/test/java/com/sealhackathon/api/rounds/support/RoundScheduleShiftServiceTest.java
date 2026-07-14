package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.ActivateScheduleMode;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundScheduleShiftServiceTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundScheduleValidator scheduleValidator;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks private RoundScheduleShiftService shiftService;

    @Test
    void keep_doesNothing() {
        Round round = Round.builder()
                .id(1)
                .examAt(LocalDateTime.now().plusDays(5))
                .codingDurationHours(7)
                .hackathon(Hackathon.builder().id(9).build())
                .build();

        assertThat(shiftService.applyOnActivate(round, ActivateScheduleMode.KEEP, null)).isFalse();
        verify(presentationSlotCascadeService, never()).rescheduleForRound(any());
        assertThat(round.getHackathon().getEventStart()).isNull();
    }

    @Test
    void startNow_compressesWindowAndReschedulesSlots() {
        LocalDateTime oldExam = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        Round round = Round.builder()
                .id(3)
                .name("Sơ loại")
                .examAt(oldExam)
                .submissionOpen(oldExam.plusHours(4))
                .submissionDeadline(oldExam.plusHours(7))
                .codingDurationHours(7)
                .deadlineReminderSentAt(LocalDateTime.now().minusDays(1))
                .hackathon(Hackathon.builder().id(9).build())
                .build();
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(scheduleValidator).validateActivateShift(any(), any(), any(), any(), eq(true));

        boolean shifted = shiftService.applyOnActivate(round, ActivateScheduleMode.START_NOW, null);

        assertThat(shifted).isTrue();
        assertThat(round.getExamAt()).isBefore(oldExam);
        assertThat(round.getDeadlineReminderSentAt()).isNull();
        assertThat(round.getSubmissionDeadline())
                .isEqualTo(RoundScheduleSeedUtil.submissionDeadline(round.getExamAt(), 7));
        verify(presentationSlotCascadeService).rescheduleForRound(3);
        verify(auditService).log(
                eq("ROUND_SCHEDULE_SHIFTED"),
                eq("rounds"),
                eq(3),
                org.mockito.ArgumentMatchers.<java.util.Map<String, Object>>any());
    }
}
