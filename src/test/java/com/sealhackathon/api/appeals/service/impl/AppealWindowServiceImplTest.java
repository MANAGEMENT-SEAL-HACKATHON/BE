package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.dto.request.PublishWithAppealWindowRequest;
import com.sealhackathon.api.appeals.dto.response.PublishPreflightResponse;
import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.appeals.value_object.AppealWindowMode;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundScheduleShiftService;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppealWindowServiceImplTest {

    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private RoundRepository roundRepository;
    @Mock private AppealRepository appealRepository;
    @Mock private AuditService auditService;
    @Mock private StakeholderBroadcastService stakeholderBroadcastService;
    @Mock private NotificationService notificationService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private RoundScheduleShiftService roundScheduleShiftService;

    @InjectMocks private AppealWindowServiceImpl service;

    private Hackathon hackathon;
    private Round prelim;
    private Round finalRound;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().withSecond(0).withNano(0);
        hackathon = Hackathon.builder().id(1).appealWindowMinutes(30).build();
        prelim = Round.builder()
                .id(10)
                .name("Sơ loại")
                .isFinal(false)
                .isPublished(true)
                .publishedAt(now)
                .publishRevision(1)
                .appealDelayMinutesApplied(0)
                .hackathon(hackathon)
                .build();
        finalRound = Round.builder()
                .id(20)
                .name("Chung kết")
                .isFinal(true)
                .examAt(now.plusMinutes(90))
                .codingDurationHours(2)
                .hackathon(hackathon)
                .build();
        when(roundAccessGuard.requireRound(10)).thenReturn(prelim);
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1)).thenReturn(Optional.of(finalRound));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findById(20)).thenReturn(Optional.of(finalRound));
        when(roundRepository.findById(10)).thenReturn(Optional.of(prelim));
    }

    @Test
    void openOnFirstPublish_setsWindowOnce_doesNotResetOnSecondCall() {
        service.openOnFirstPublish(prelim, null, now);

        assertThat(prelim.getAppealWindowEndsAt()).isEqualTo(now.plusMinutes(30));

        LocalDateTime firstEnds = prelim.getAppealWindowEndsAt();
        service.openOnFirstPublish(prelim, null, now.plusMinutes(5));
        assertThat(prelim.getAppealWindowEndsAt()).isEqualTo(firstEnds);
        verify(roundScheduleShiftService, never()).delayFinalForAppeals(any(), any(), anyInt());
    }

    @Test
    void openOnFirstPublish_whenFits_emptyBodyWorks() {
        PublishPreflightResponse pre = service.preflight(10);
        assertThat(pre.isFits()).isTrue();

        service.openOnFirstPublish(prelim, null, now);
        assertThat(prelim.getAppealWindowEndsAt()).isEqualTo(now.plusMinutes(30));
    }

    @Test
    void openOnFirstPublish_lateWithoutMode_throwsDoesNotFit() {
        finalRound.setExamAt(now.plusMinutes(15));

        assertThatThrownBy(() -> service.openOnFirstPublish(prelim, null, now))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_WINDOW_DOES_NOT_FIT);
    }

    @Test
    void latePublish_delayFinal_callsShiftService() {
        finalRound.setExamAt(now.plusMinutes(15));
        PublishWithAppealWindowRequest req = PublishWithAppealWindowRequest.builder()
                .appealWindowMode(AppealWindowMode.DELAY_FINAL)
                .build();
        when(roundScheduleShiftService.delayFinalForAppeals(eq(prelim), eq(finalRound), eq(15)))
                .thenAnswer(inv -> {
                    finalRound.setExamAt(now.plusMinutes(45));
                    return finalRound.getExamAt();
                });

        service.openOnFirstPublish(prelim, req, now);

        verify(roundScheduleShiftService).delayFinalForAppeals(prelim, finalRound, 15);
        assertThat(prelim.getAppealWindowEndsAt()).isEqualTo(now.plusMinutes(30));
    }

    @Test
    void latePublish_shrink_whenRemainingAboveMin() {
        finalRound.setExamAt(now.plusMinutes(15));
        PublishWithAppealWindowRequest req = PublishWithAppealWindowRequest.builder()
                .appealWindowMode(AppealWindowMode.SHRINK)
                .build();

        service.openOnFirstPublish(prelim, req, now);

        assertThat(prelim.getAppealWindowEndsAt()).isEqualTo(now.plusMinutes(15));
        verify(roundScheduleShiftService, never()).delayFinalForAppeals(any(), any(), anyInt());
    }

    @Test
    void latePublish_shrink_blockedUnder10Minutes() {
        finalRound.setExamAt(now.plusMinutes(6));
        PublishWithAppealWindowRequest req = PublishWithAppealWindowRequest.builder()
                .appealWindowMode(AppealWindowMode.SHRINK)
                .build();

        assertThatThrownBy(() -> service.openOnFirstPublish(prelim, req, now))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_WINDOW_BELOW_MINIMUM);
    }

    @Test
    void latePublish_skip_requiresReason_andDoesNotOpenWindow() {
        finalRound.setExamAt(now.plusMinutes(15));
        PublishWithAppealWindowRequest req = PublishWithAppealWindowRequest.builder()
                .appealWindowMode(AppealWindowMode.SKIP)
                .skipReason("Chấm quá muộn")
                .build();

        service.openOnFirstPublish(prelim, req, now);

        assertThat(prelim.getAppealWindowEndsAt()).isNull();
    }

    @Test
    void preflight_listsModes_shrinkBlockedUnder10() {
        finalRound.setExamAt(now.plusMinutes(6));
        PublishPreflightResponse pre = service.preflight(10);

        assertThat(pre.isFits()).isFalse();
        assertThat(pre.getAvailableModes()).hasSize(3);
        PublishPreflightResponse.ModeAvailability shrink = pre.getAvailableModes().stream()
                .filter(m -> m.getMode() == AppealWindowMode.SHRINK)
                .findFirst().orElseThrow();
        assertThat(shrink.isAvailable()).isFalse();
    }

    @Test
    void expireOpenAppeals_setsExpired() {
        prelim.setAppealWindowEndsAt(now.minusMinutes(1));
        Team team = Team.builder().id(5).teamName("A").build();
        Appeal appeal = Appeal.builder()
                .id(99)
                .team(team)
                .round(prelim)
                .status(AppealStatus.PENDING)
                .build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(prelim));
        when(appealRepository.findByRoundIdAndStatusIn(eq(10), any())).thenReturn(List.of(appeal));
        when(teamMemberRepository.findByTeam_Id(5)).thenReturn(List.of());

        int n = service.expireOpenAppealsForRound(10);

        assertThat(n).isEqualTo(1);
        assertThat(appeal.getStatus()).isEqualTo(AppealStatus.EXPIRED);
    }

    @Test
    void republish_incrementsRevision_doesNotResetWindow() {
        LocalDateTime ends = now.plusMinutes(20);
        prelim.setAppealWindowEndsAt(ends);
        prelim.setPublishRevision(1);

        service.republish(10);

        assertThat(prelim.getPublishRevision()).isEqualTo(2);
        assertThat(prelim.getAppealWindowEndsAt()).isEqualTo(ends);
        assertThat(prelim.getResultsRevisedAt()).isNotNull();
    }

    @Test
    void zeroWindowMinutes_doesNotOpen() {
        hackathon.setAppealWindowMinutes(0);
        service.openOnFirstPublish(prelim, null, now);
        assertThat(prelim.getAppealWindowEndsAt()).isNull();
    }
}
