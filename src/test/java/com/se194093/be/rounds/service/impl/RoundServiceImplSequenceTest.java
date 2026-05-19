package com.se194093.be.rounds.service.impl;

import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.judge_assignments.repository.JudgeAssignmentRepository;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.mapper.RoundMapper;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.rounds.value_object.RoundType;
import com.se194093.be.submissions.repository.SubmissionPlaceholderRepository;
import com.se194093.be.tracks.repository.TrackRepository;
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
