package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.service.MilestoneEventRescheduleService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonRoundTimelineSyncService;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundScheduleShiftServiceDelayFinalTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundScheduleValidator scheduleValidator;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;
    @Mock private AuditService auditService;
    @Mock private StakeholderBroadcastService stakeholderBroadcastService;
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private MilestoneEventRescheduleService milestoneEventRescheduleService;
    @Mock private HackathonRepository hackathonRepository;

    @InjectMocks private RoundScheduleShiftService shiftService;

    private Hackathon hackathon;
    private Round prelim;
    private Round finalRound;
    private LocalDateTime examAt;

    @BeforeEach
    void setUp() {
        examAt = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
        hackathon = Hackathon.builder()
                .id(9)
                .eventEnd(LocalDate.now().plusDays(2))
                .build();
        prelim = Round.builder()
                .id(3)
                .isFinal(false)
                .isPublished(true)
                .appealDelayMinutesApplied(0)
                .hackathon(hackathon)
                .build();
        finalRound = Round.builder()
                .id(4)
                .name("Chung kết")
                .isFinal(true)
                .isActive(false)
                .examAt(examAt)
                .codingDurationHours(2)
                .submissionOpen(RoundScheduleSeedUtil.submissionOpen(examAt, 2))
                .submissionDeadline(RoundScheduleSeedUtil.submissionDeadline(examAt, 2))
                .hackathon(hackathon)
                .build();
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(hackathonRepository.findById(9)).thenReturn(Optional.of(hackathon));
        lenient().doNothing().when(scheduleValidator).requireNewExamAtNotInPast(any(), any());
        lenient().doNothing().when(scheduleValidator).validateActivateShift(
                any(), any(), any(), any(), eq(true), anyBoolean());
        lenient().when(milestoneEventRescheduleService.repositionAwardsAfterFinal(any(), any())).thenReturn(1);
    }

    @Test
    void delayFinal_appliesMinutes_andCascadesAwards() {
        LocalDateTime neu = shiftService.delayFinalForAppeals(prelim, finalRound, 15);

        assertThat(neu).isEqualTo(RoundScheduleClocks.ceilToNextMinute(examAt.plusMinutes(15)));
        assertThat(prelim.getAppealDelayMinutesApplied()).isEqualTo(15);
        verify(hackathonRoundTimelineSyncService).syncFromRounds(9);
        verify(milestoneEventRescheduleService).repositionAwardsAfterFinal(hackathon, finalRound);
        verify(presentationSlotCascadeService).rescheduleForRound(4);
    }

    @Test
    void delayFinal_cumulativeCapAcrossTwoCalls() {
        shiftService.delayFinalForAppeals(prelim, finalRound, 20);
        assertThat(prelim.getAppealDelayMinutesApplied()).isEqualTo(20);

        assertThatThrownBy(() -> shiftService.delayFinalForAppeals(prelim, finalRound, 15))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_DELAY_LIMIT_EXCEEDED);
    }

    @Test
    void delayFinal_blocksPastEventEnd() {
        hackathon.setEventEnd(LocalDate.now()); // today — shifting +hours may exceed
        finalRound.setExamAt(LocalDateTime.now().plusMinutes(30).withHour(23).withMinute(50).withSecond(0).withNano(0));

        assertThatThrownBy(() -> shiftService.delayFinalForAppeals(prelim, finalRound, 30))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.EVENT_OUT_OF_HACKATHON);
    }
}
