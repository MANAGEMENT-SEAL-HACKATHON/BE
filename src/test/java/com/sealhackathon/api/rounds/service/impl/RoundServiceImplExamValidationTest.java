package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.submissions.repository.SubmissionPlaceholderRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    @Mock private SubmissionPlaceholderRepository submissionRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private HackathonTimelineService hackathonTimelineService;
    @Mock private EventRepository eventRepository;

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
