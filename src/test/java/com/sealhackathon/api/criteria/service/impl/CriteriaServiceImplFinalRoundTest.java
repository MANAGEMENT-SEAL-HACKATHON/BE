package com.sealhackathon.api.criteria.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.criteria.dto.request.CreateCriterionRequest;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.mapper.CriteriaMapper;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chung kết: criteria gắn {@code round_id}, không cần Track con.
 */
@ExtendWith(MockitoExtension.class)
class CriteriaServiceImplFinalRoundTest {

    @Mock private CriteriaRepository criteriaRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private CriteriaMapper criteriaMapper;
    @Mock private AuditService auditService;
    @Mock private WeightSummaryService weightSummaryService;
    @Mock private ScoreRepository scoreRepository;

    @InjectMocks
    private CriteriaServiceImpl criteriaService;

    @Test
    void createForFinalRound_succeedsWithoutTracks() {
        Round finalRound = Round.builder().id(99).isFinal(true).name("Chung kết").build();
        when(roundRepository.findById(99)).thenReturn(Optional.of(finalRound));

        CreateCriterionRequest req = CreateCriterionRequest.builder()
                .name("Xử lý & Truy xuất")
                .type(CriteriaType.TECHNICAL)
                .weight(0.3f)
                .maxScore(10)
                .build();

        Criteria entity = Criteria.builder().id(1).round(finalRound).track(null).name(req.getName()).build();
        when(criteriaMapper.toEntityForFinalRound(req, finalRound)).thenReturn(entity);
        when(criteriaRepository.save(entity)).thenReturn(entity);
        when(criteriaMapper.toResponse(entity)).thenReturn(
                com.sealhackathon.api.criteria.dto.response.CriterionResponse.builder()
                        .id(1)
                        .roundId(99)
                        .trackId(null)
                        .name(req.getName())
                        .type(req.getType())
                        .weight(req.getWeight())
                        .maxScore(req.getMaxScore())
                        .build());

        var result = criteriaService.createForFinalRound(99, req);

        assertNotNull(result.criterion());
        assertNotNull(result.criterion().getRoundId());
        verify(trackRepository, org.mockito.Mockito.never()).findById(any());
        verify(criteriaRepository).save(entity);
    }
}
