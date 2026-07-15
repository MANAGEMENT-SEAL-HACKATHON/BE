package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonRoundTimelineSyncService;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundServiceImplExamValidationTest {

    @Mock private RoundRepository roundRepository;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private AuditService auditService;
    @Mock private WeightSummaryService weightSummaryService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private HackathonTimelineService hackathonTimelineService;
    @Mock private EventRepository eventRepository;
    @Spy private HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private com.sealhackathon.api.presentation.service.PresentationSlotCascadeService presentationSlotCascadeService;

    @InjectMocks
    private RoundServiceImpl roundService;

    private static final LocalDateTime DEADLINE = LocalDateTime.now().plusDays(30);

    @Test
    void createFinal_blocksWhenExamNotAfterPreliminary() {
        mockHackathon();
        LocalDateTime prelimExam = LocalDateTime.now().plusDays(20);
        when(roundRepository.findPreliminaryLikeByHackathonId(1))
                .thenReturn(List.of(Round.builder().id(10).build()));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);
        when(roundRepository.maxExamAtNonFinal(1)).thenReturn(Optional.of(prelimExam));

        CreateRoundRequest req = finalRequest(prelimExam.minusDays(1));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_EXAM_ORDER, ex.getCode());
    }

    @Test
    void createFinal_blocksWithoutPreliminaryRound() {
        mockHackathon();
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of());
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, finalRequest(LocalDateTime.now().plusDays(40))));
        assertEquals(ErrorCode.ROUND_FINAL_REQUIRES_PRELIM, ex.getCode());
    }

    @Test
    void createFinal_blocksDuplicateFinal() {
        mockHackathon();
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(1L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> roundService.createByHackathon(1, finalRequest(LocalDateTime.now().plusDays(40))));
        assertEquals(ErrorCode.ROUND_DUPLICATE_FINAL, ex.getCode());
    }

    @Test
    void createPreliminary_blocksExamOnOrAfterFinal() {
        mockHackathon();
        LocalDateTime finalExam = LocalDateTime.now().plusDays(25);
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1))
                .thenReturn(Optional.of(Round.builder().id(99).examAt(finalExam).build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại 2")
                .examAt(finalExam)
                .isFinal(false)
                .submissionDeadline(DEADLINE)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_PRELIM_EXAM_ORDER, ex.getCode());
    }

    @Test
    void createPreliminary_blocksExamOnOrAfterSubmissionOpen() {
        mockHackathon();
        LocalDateTime open = LocalDateTime.now().plusDays(10);
        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(open)
                .submissionOpen(open)
                .submissionDeadline(DEADLINE)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_EXAM_BEFORE_SUBMISSION_OPEN, ex.getCode());
    }

    @Test
    void createPreliminary_delegatesTimelineValidation() {
        mockHackathon();
        LocalDateTime examAt = LocalDateTime.of(2026, 4, 12, 8, 0);
        doThrow(new BusinessRuleException(ErrorCode.ROUND_EXAM_BEFORE_KICKOFF, "kickoff", null))
                .when(hackathonTimelineService).validateRoundExamAt(1, false, examAt);

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(examAt)
                .submissionOpen(examAt.plusHours(1))
                .submissionDeadline(DEADLINE)
                .build();

        assertThrows(BusinessRuleException.class, () -> roundService.createByHackathon(1, req));
        verify(hackathonTimelineService).validateRoundExamAt(1, false, examAt);
    }

    @Test
    void updatePreliminary_blocksExamAfterFinal() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        LocalDateTime finalExam = LocalDateTime.now().plusDays(30);
        Round prelim = Round.builder().id(5).hackathon(h).isFinal(false).build();
        when(roundRepository.findById(5)).thenReturn(Optional.of(prelim));
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1))
                .thenReturn(Optional.of(Round.builder().id(99).examAt(finalExam).build()));

        UpdateRoundRequest req = UpdateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(finalExam.plusDays(1))
                .submissionDeadline(DEADLINE)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.update(5, req));
        assertEquals(ErrorCode.ROUND_PRELIM_EXAM_ORDER, ex.getCode());
        verify(roundRepository, never()).save(any());
    }

    @Test
    void createPreliminary_blocksDeadlineOnOrAfterFinalExam() {
        mockHackathon();
        LocalDateTime finalExam = LocalDateTime.now().plusDays(25);
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1))
                .thenReturn(Optional.of(Round.builder().id(99).examAt(finalExam).build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(finalExam.minusDays(2))
                .submissionOpen(finalExam.minusDays(2).plusHours(1))
                .submissionDeadline(finalExam)
                .isFinal(false)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_PRELIM_DEADLINE_AFTER_FINAL_EXAM, ex.getCode());
    }

    @Test
    void createFinal_blocksWhenAwardsStartsBeforeSubmissionDeadline() {
        mockHackathon();
        LocalDateTime day = LocalDateTime.now().plusDays(40).withHour(0).withMinute(0).withSecond(0).withNano(0);
        when(roundRepository.findPreliminaryLikeByHackathonId(1))
                .thenReturn(List.of(Round.builder().id(10).build()));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);
        when(roundRepository.maxExamAtNonFinal(1))
                .thenReturn(Optional.of(day.withHour(8)));
        when(eventRepository.findByHackathonIdAndType(eq(1), any()))
                .thenReturn(List.of(com.sealhackathon.api.events.entity.Event.builder()
                        .startsAt(day.withHour(10))
                        .endsAt(day.withHour(11))
                        .build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(day.withHour(13))
                .submissionOpen(day.withHour(14))
                .submissionDeadline(day.withHour(16).withMinute(30))
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_DEADLINE_AFTER_AWARDS, ex.getCode());
    }

    @Test
    void createFinal_blocksDeadlineOnOrAfterAwards() {
        mockHackathon();
        LocalDateTime awardsStart = LocalDateTime.now().plusDays(40);
        when(roundRepository.findPreliminaryLikeByHackathonId(1))
                .thenReturn(List.of(Round.builder().id(10).build()));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);
        when(roundRepository.maxExamAtNonFinal(1))
                .thenReturn(Optional.of(LocalDateTime.now().plusDays(20)));
        when(eventRepository.findByHackathonIdAndType(eq(1), any()))
                .thenReturn(List.of(com.sealhackathon.api.events.entity.Event.builder()
                        .startsAt(awardsStart)
                        .build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(awardsStart.minusHours(4))
                .submissionOpen(awardsStart.minusHours(3))
                .submissionDeadline(awardsStart)
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_DEADLINE_AFTER_AWARDS, ex.getCode());
    }

    @Test
    void createPreliminary_blocksWhenEarlierThanRegistrationEndPlusFiveDays() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .status(HackathonStatus.DRAFT)
                .registrationEnd(java.time.LocalDate.of(2026, 6, 5))
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(LocalDateTime.of(2026, 6, 9, 8, 0))
                .submissionOpen(LocalDateTime.of(2026, 6, 9, 9, 0))
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .isFinal(false)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_PRELIM_EXAM_ORDER, ex.getCode());
    }

    @Test
    void createPreliminary_allowsOnRegistrationEndPlusFiveDays() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .status(HackathonStatus.DRAFT)
                .registrationEnd(java.time.LocalDate.of(2026, 6, 5))
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));
        when(roundMapper.toEntity(any(), eq(h))).thenReturn(Round.builder().id(10).hackathon(h).build());
        when(roundRepository.save(any())).thenReturn(Round.builder().id(10).hackathon(h).build());
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(10).name("Sơ loại").build());

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(LocalDateTime.of(2026, 6, 10, 8, 0))
                .submissionOpen(LocalDateTime.of(2026, 6, 10, 9, 0))
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .isFinal(false)
                .build();

        assertDoesNotThrow(() -> roundService.createByHackathon(1, req));
    }

    @Test
    void createFinal_allowsAfterGradingBufferWhenCodingDurationProvided() {
        mockHackathon();
        LocalDateTime prelimExam = LocalDateTime.of(2026, 6, 21, 8, 0);
        Round prelim = Round.builder()
                .id(10)
                .examAt(prelimExam)
                .codingDurationHours(8)
                .build();
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of(prelim));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);
        when(roundMapper.toEntity(any(), any())).thenReturn(Round.builder().id(99).build());
        when(roundRepository.save(any())).thenReturn(Round.builder().id(99).build());
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(99).name("Chung kết").build());

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(LocalDateTime.of(2026, 6, 21, 19, 0))
                .submissionOpen(LocalDateTime.of(2026, 6, 21, 19, 30))
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .build();

        assertDoesNotThrow(() -> roundService.createByHackathon(1, req));
    }

    @Test
    void createFinal_blocksBeforeGradingBufferEndsWhenCodingDurationProvided() {
        mockHackathon();
        LocalDateTime prelimExam = LocalDateTime.of(2026, 6, 21, 8, 0);
        Round prelim = Round.builder()
                .id(10)
                .examAt(prelimExam)
                .codingDurationHours(8)
                .build();
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of(prelim));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(LocalDateTime.of(2026, 6, 21, 18, 59))
                .submissionOpen(LocalDateTime.of(2026, 6, 21, 19, 30))
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_EXAM_ORDER, ex.getCode());
    }

    @Test
    void createFinal_blocksBeforePreliminaryEndWhenCodingDurationProvided() {
        mockHackathon();
        LocalDateTime prelimExam = LocalDateTime.of(2026, 6, 21, 8, 0);
        Round prelim = Round.builder()
                .id(10)
                .examAt(prelimExam)
                .codingDurationHours(8)
                .build();
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of(prelim));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(LocalDateTime.of(2026, 6, 21, 16, 0))
                .submissionOpen(LocalDateTime.of(2026, 6, 21, 16, 30))
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_EXAM_ORDER, ex.getCode());
    }

    @Test
    void createPreliminary_withSixHourDuration_requiresOpenAtTwoThirdsAndDeadlineAtEnd() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .status(HackathonStatus.DRAFT)
                .registrationEnd(java.time.LocalDate.of(2026, 10, 21))
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));
        when(roundMapper.toEntity(any(), eq(h))).thenReturn(Round.builder().id(11).hackathon(h).build());
        when(roundRepository.save(any())).thenReturn(Round.builder().id(11).hackathon(h).build());
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(11).name("Sơ loại").build());

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(LocalDateTime.of(2026, 10, 26, 8, 0))
                .codingDurationHours(6)
                .submissionOpen(LocalDateTime.of(2026, 10, 26, 12, 0))
                .submissionDeadline(LocalDateTime.of(2026, 10, 26, 14, 0))
                .isFinal(false)
                .build();

        assertDoesNotThrow(() -> roundService.createByHackathon(1, req));
    }

    @Test
    void createPreliminary_blocksSubmissionDeadlineBeforeOpen() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .status(HackathonStatus.DRAFT)
                .registrationEnd(java.time.LocalDate.of(2026, 10, 21))
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(LocalDateTime.of(2026, 10, 26, 8, 0))
                .submissionOpen(LocalDateTime.of(2026, 10, 26, 14, 0))
                .submissionDeadline(LocalDateTime.of(2026, 10, 26, 12, 0))
                .isFinal(false)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_DEADLINE_INVALID, ex.getCode());
    }

    @Test
    void createPreliminary_withHardLockPolicy_isAllowedWhenOtherRulesValid() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .status(HackathonStatus.DRAFT)
                .registrationEnd(java.time.LocalDate.of(2026, 6, 4))
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));
        when(roundMapper.toEntity(any(), eq(h))).thenReturn(Round.builder().id(12).hackathon(h).build());
        when(roundRepository.save(any())).thenReturn(Round.builder().id(12).hackathon(h).build());
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(12).name("test").build());

        LocalDateTime examAt = LocalDateTime.now().plusDays(10).withHour(8).withMinute(0).withSecond(0).withNano(0);

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("test")
                .examAt(examAt)
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .codingDurationHours(5)
                .lateSubmissionPolicy(com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy.HARD_LOCK)
                .problemReleasedAt(examAt)
                .submissionOpen(examAt.plusHours(3).plusMinutes(20))
                .submissionDeadline(examAt.plusHours(5))
                .wildcardEnabled(false)
                .tiebreakRule(com.sealhackathon.api.rounds.value_object.TiebreakRule.PENALTY_SCORE)
                .build();

        assertDoesNotThrow(() -> roundService.createByHackathon(1, req));
    }

    @Test
    void update_activeRound_rejectsScheduleChange() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).build();
        LocalDateTime examAt = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime open = examAt.plusHours(3);
        LocalDateTime deadline = examAt.plusHours(5);
        Round active = Round.builder()
                .id(10)
                .hackathon(h)
                .name("Sơ loại")
                .isActive(true)
                .examAt(examAt)
                .submissionOpen(open)
                .submissionDeadline(deadline)
                .build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(active));

        UpdateRoundRequest req = UpdateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(examAt.plusHours(2))
                .submissionOpen(open.plusHours(2))
                .submissionDeadline(deadline.plusHours(2))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.update(10, req));
        assertEquals(ErrorCode.INVALID_STATE, ex.getCode());
        verify(roundRepository, never()).save(any());
    }

    private void mockHackathon() {
        when(hackathonRepository.findById(1))
                .thenReturn(Optional.of(Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build()));
        when(eventRepository.findByHackathonIdAndType(any(), any())).thenReturn(Collections.emptyList());
    }

    private static CreateRoundRequest finalRequest(LocalDateTime examAt) {
        return CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(examAt)
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .submissionDeadline(DEADLINE)
                .build();
    }
}
