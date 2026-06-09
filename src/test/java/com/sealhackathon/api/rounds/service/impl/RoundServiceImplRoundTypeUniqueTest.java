package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonRoundTimelineSyncService;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * FR-06A v3.2 — mỗi {@code roundType} chỉ tạo 1 lần / Hackathon (PRELIMINARY / SEMIFINAL / FINAL).
 */
@ExtendWith(MockitoExtension.class)
class RoundServiceImplRoundTypeUniqueTest {

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
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;
    @Spy private HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks
    private RoundServiceImpl roundService;

    private static final LocalDateTime DEADLINE = LocalDateTime.now().plusDays(30);

    @Test
    void createPreliminary_blocksWhenAnotherPreliminaryExists() {
        when(hackathonRepository.findById(1))
                .thenReturn(Optional.of(Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build()));
        when(roundRepository.findByHackathon_IdAndRoundType(1, RoundType.PRELIMINARY))
                .thenReturn(List.of(Round.builder().id(1).build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại 2")
                .examAt(LocalDateTime.now().plusDays(20))
                .submissionDeadline(DEADLINE)
                .roundType(RoundType.PRELIMINARY)
                .build();

        ConflictException ex = assertThrows(ConflictException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_TYPE_DUPLICATE, ex.getCode());
    }

    @Test
    void createPreliminary_defaultRoundTypeIsBlockedWhenExists() {
        when(hackathonRepository.findById(1))
                .thenReturn(Optional.of(Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build()));
        when(roundRepository.findByHackathon_IdAndRoundType(1, RoundType.PRELIMINARY))
                .thenReturn(List.of(Round.builder().id(1).build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Sơ loại 2")
                .examAt(LocalDateTime.now().plusDays(20))
                .submissionDeadline(DEADLINE)
                .build();

        ConflictException ex = assertThrows(ConflictException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_TYPE_DUPLICATE, ex.getCode());
    }

    @Test
    void createSemifinal_blocksWhenSemifinalExists() {
        when(hackathonRepository.findById(1))
                .thenReturn(Optional.of(Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build()));
        when(roundRepository.findByHackathon_IdAndRoundType(1, RoundType.SEMIFINAL))
                .thenReturn(List.of(Round.builder().id(1).build()));

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Bán kết 2")
                .examAt(LocalDateTime.now().plusDays(20))
                .submissionDeadline(DEADLINE)
                .roundType(RoundType.SEMIFINAL)
                .build();

        ConflictException ex = assertThrows(ConflictException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_TYPE_DUPLICATE, ex.getCode());
    }
}
