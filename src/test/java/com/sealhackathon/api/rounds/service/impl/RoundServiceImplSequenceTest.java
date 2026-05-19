package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceImplSequenceTest {

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

    @InjectMocks
    private RoundServiceImpl roundService;

    @Test
    void blocksFinalRoundWithSequenceNotAfterPreliminary() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));
        when(roundRepository.maxSequenceOrderNonFinal(1)).thenReturn(1);

        CreateRoundRequest req = CreateRoundRequest.builder()
                .name("Chung kết")
                .sequenceOrder(1)
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> roundService.createByHackathon(1, req));
        assertEquals(ErrorCode.ROUND_FINAL_SEQUENCE_ORDER, ex.getCode());
    }
}
