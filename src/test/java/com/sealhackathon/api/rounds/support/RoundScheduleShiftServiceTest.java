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
import java.time.temporal.ChronoUnit;

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

    private Round prelimRound(LocalDateTime oldExam, int codingHours) {
        return Round.builder()
                .id(3)
                .name("Sơ loại")
                .examAt(oldExam)
                .submissionOpen(RoundScheduleSeedUtil.submissionOpen(oldExam, codingHours))
                .submissionDeadline(RoundScheduleSeedUtil.submissionDeadline(oldExam, codingHours))
                .codingDurationHours(codingHours)
                .deadlineReminderSentAt(LocalDateTime.now().minusDays(1))
                .hackathon(Hackathon.builder().id(9).build())
                .build();
    }

    @Test
    void keep_doesNothing() {
        Round round = Round.builder()
                .id(1)
                .examAt(LocalDateTime.now().plusDays(5))
                .codingDurationHours(7)
                .hackathon(Hackathon.builder().id(9).build())
                .build();

        assertThat(shiftService.applyOnActivate(round, ActivateScheduleMode.KEEP, null, null)).isFalse();
        verify(presentationSlotCascadeService, never()).rescheduleForRound(any());
    }

    /** TC-BE-01 — lead=10, không ceil */
    @Test
    void startNow_withLead10_setsExamNearNowPlus10Exact() {
        LocalDateTime oldExam = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        Round round = prelimRound(oldExam, 7);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(scheduleValidator).validateActivateShift(any(), any(), any(), any(), eq(true));

        LocalDateTime before = LocalDateTime.now();
        boolean shifted = shiftService.applyOnActivate(
                round, ActivateScheduleMode.START_NOW, null, 10);
        LocalDateTime after = LocalDateTime.now();

        assertThat(shifted).isTrue();
        assertThat(round.getExamAt()).isAfterOrEqualTo(before.plusMinutes(10).minusSeconds(2));
        assertThat(round.getExamAt()).isBeforeOrEqualTo(after.plusMinutes(10).plusSeconds(2));
        assertThat(round.getExamAt()).isBefore(oldExam);
        verify(presentationSlotCascadeService).rescheduleForRound(3);
    }

    /** TC-BE-02 — null lead → default 5, không ceil */
    @Test
    void startNow_nullLead_defaultsToFiveMinutesExact() {
        LocalDateTime oldExam = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        Round round = prelimRound(oldExam, 7);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(scheduleValidator).validateActivateShift(any(), any(), any(), any(), eq(true));

        LocalDateTime before = LocalDateTime.now();
        shiftService.applyOnActivate(round, ActivateScheduleMode.START_NOW, null, null);
        LocalDateTime after = LocalDateTime.now();

        assertThat(round.getExamAt()).isAfterOrEqualTo(before.plusMinutes(5).minusSeconds(2));
        assertThat(round.getExamAt()).isBeforeOrEqualTo(after.plusMinutes(5).plusSeconds(2));
    }

    /** TC-BE-03 */
    @Test
    void startNow_preservesCodingDurationWindow() {
        LocalDateTime oldExam = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        Round round = prelimRound(oldExam, 7);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(scheduleValidator).validateActivateShift(any(), any(), any(), any(), eq(true));

        shiftService.applyOnActivate(round, ActivateScheduleMode.START_NOW, null, 5);

        assertThat(round.getSubmissionDeadline())
                .isEqualTo(RoundScheduleSeedUtil.submissionDeadline(round.getExamAt(), 7));
        assertThat(round.getSubmissionOpen())
                .isEqualTo(RoundScheduleSeedUtil.submissionOpen(round.getExamAt(), 7));
        assertThat(ChronoUnit.HOURS.between(round.getExamAt(), round.getSubmissionDeadline())).isEqualTo(7);
    }

    /** TC-BE-05 */
    @Test
    void reschedule_setsNewExamAndRecalculatesWindow() {
        LocalDateTime oldExam = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        LocalDateTime newExam = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        Round round = prelimRound(oldExam, 7);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(scheduleValidator).requireNewExamAtNotInPast(any(), any());
        doNothing().when(scheduleValidator).validateActivateShift(any(), any(), any(), any(), eq(true));

        boolean shifted = shiftService.applyOnActivate(
                round, ActivateScheduleMode.RESCHEDULE, newExam, null);

        assertThat(shifted).isTrue();
        assertThat(round.getExamAt()).isEqualTo(RoundScheduleClocks.ceilToNextMinute(newExam));
        assertThat(round.getSubmissionDeadline())
                .isEqualTo(RoundScheduleSeedUtil.submissionDeadline(round.getExamAt(), 7));
        verify(presentationSlotCascadeService).rescheduleForRound(3);
    }

    /** Không ceil khi có giây */
    @Test
    void startNow_keepsSeconds_noCeil() {
        LocalDateTime oldExam = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        Round round = prelimRound(oldExam, 7);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(scheduleValidator).validateActivateShift(any(), any(), any(), any(), eq(true));

        LocalDateTime before = LocalDateTime.now();
        shiftService.applyOnActivate(round, ActivateScheduleMode.START_NOW, null, 5);

        // examAt ≈ before+5 (không ceil thêm phút)
        long diffSec = ChronoUnit.SECONDS.between(before.plusMinutes(5), round.getExamAt());
        assertThat(Math.abs(diffSec)).isLessThanOrEqualTo(2);
    }
}
