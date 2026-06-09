package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonRoundTimelineSyncService;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FR-03 DELETE — Round Chung kết có Criteria cấu hình (chưa chấm) vẫn được xóa.
 */
@ExtendWith(MockitoExtension.class)
class RoundServiceImplDeleteTest {

    @Mock private RoundRepository roundRepository;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private AuditService auditService;
    @Mock private WeightSummaryService weightSummaryService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private HackathonTimelineService hackathonTimelineService;
    @Mock private EventRepository eventRepository;
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;
    @Spy private HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks
    private RoundServiceImpl roundService;

    @Test
    void deleteFinalRound_withCriteriaConfig_succeedsEvenIfNativeDeletePartial() {
        Round round = Round.builder()
                .id(99)
                .name("Chung kết")
                .isFinal(true)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build())
                .build();
        when(roundRepository.findById(99)).thenReturn(Optional.of(round));
        when(submissionRepository.countByRoundId(99)).thenReturn(0L);
        when(scoreRepository.countByRoundId(99)).thenReturn(0L);
        when(criteriaRepository.countAllLinkedToRoundNative(99)).thenReturn(3L, 1L);
        when(trackRepository.countByRoundId(99)).thenReturn(0L);
        when(criteriaRepository.deleteAllLinkedToRoundNative(99)).thenReturn(2);
        when(judgeAssignmentRepository.findByRoundId(99)).thenReturn(Collections.emptyList());
        when(roundMapper.toResponse(round)).thenReturn(RoundResponse.builder().id(99).build());

        Integer deleted = roundService.delete(99);

        assertEquals(99, deleted);
        verify(criteriaRepository).unlinkSourceReferencingRound(99);
        verify(criteriaRepository).deleteAllLinkedToRoundNative(99);
        verify(roundRepository).delete(round);
    }

    @Test
    void delete_blocksOnlyWhenScoresExist() {
        Round round = Round.builder()
                .id(5)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build())
                .build();
        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(submissionRepository.countByRoundId(5)).thenReturn(0L);
        when(scoreRepository.countByRoundId(5)).thenReturn(2L);

        ConflictException ex = assertThrows(ConflictException.class, () -> roundService.delete(5));
        assertEquals(ErrorCode.ROUND_HAS_CRITERIA, ex.getCode());
        verify(roundRepository, never()).delete(any());
    }
}
