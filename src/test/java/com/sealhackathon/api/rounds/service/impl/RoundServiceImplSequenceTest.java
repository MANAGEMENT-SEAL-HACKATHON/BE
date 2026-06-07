package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundServiceImplSequenceTest {

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

    @InjectMocks
    private RoundServiceImpl roundService;

    @Test
    void blocksFinalRoundWhenExamNotAfterPreliminary() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        LocalDateTime prelimExam = LocalDateTime.now().plusDays(20);
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));
        when(roundRepository.findPreliminaryLikeByHackathonId(1))
                .thenReturn(List.of(Round.builder().id(10).build()));
        when(roundRepository.countByHackathon_IdAndIsFinalTrue(1)).thenReturn(0L);
        when(roundRepository.maxExamAtNonFinal(1)).thenReturn(Optional.of(prelimExam));
        when(eventRepository.findByHackathonIdAndType(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .examAt(prelimExam.minusDays(1))
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_EXAM_ORDER, ex.getCode());
    }
}
