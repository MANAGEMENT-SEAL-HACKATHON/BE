package com.sealhackathon.api.rbl.service.impl;

import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.rbl.dto.response.RblScoringProgressResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RblDashboardServiceImpl implements RblDashboardService {

    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final RoundRepository roundRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RblVarianceItemResponse> varianceByRound(Integer roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("Round", roundId);
        }
        List<RblVarianceItemResponse> calibration = queryVariance(roundId, ScoreType.CALIBRATION);
        if (!calibration.isEmpty()) {
            return calibration;
        }
        return queryVariance(roundId, ScoreType.NORMAL);
    }

    private List<RblVarianceItemResponse> queryVariance(Integer roundId, ScoreType scoreType) {
        String sql = """
            SELECT 
                c.id as criterionId,
                c.name as criterionName,
                c.type as criterionType,
                s.judge_id as judgeId,
                u.user_type as judgeType,
                AVG(s.score_value) as meanScore,
                STDDEV(s.score_value) as stdDev
            FROM scores s
            JOIN criteria c ON s.criterion_id = c.id
            JOIN users u ON s.judge_id = u.id
            JOIN submissions sub ON s.submission_id = sub.id
            WHERE sub.round_id = ?1 
              AND s.score_type = ?2
            GROUP BY c.id, c.name, c.type, s.judge_id, u.user_type
            ORDER BY c.id ASC, stdDev DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, roundId);
        query.setParameter(2, scoreType.name());

        List<Object[]> results = query.getResultList();
        List<RblVarianceItemResponse> responses = new ArrayList<>();

        for (Object[] row : results) {
            responses.add(RblVarianceItemResponse.builder()
                    .criterionId((Integer) row[0])
                    .criterionName((String) row[1])
                    .criterionType((String) row[2])
                    .judgeId((Integer) row[3])
                    .judgeType((String) row[4])
                    .meanScore(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                    .stdDev(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0)
                    .build());
        }

        return responses;
    }

    @Override
    public RblScoringProgressResponse scoringProgress(Integer roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("Round", roundId);
        }

        long totalSubmissions = submissionRepository.countByRoundId(roundId);

        // Đã đổi :roundId và :scoreType thành ?1 và ?2
        String sql = """
            SELECT COUNT(DISTINCT s.submission_id)
            FROM scores s
            JOIN submissions sub ON s.submission_id = sub.id
            WHERE sub.round_id = ?1 
              AND s.score_type = ?2
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, roundId);
        query.setParameter(2, ScoreType.NORMAL.name());

        Number scoredCountNum = (Number) query.getSingleResult();
        long scoredSubmissions = scoredCountNum != null ? scoredCountNum.longValue() : 0;

        double completionPct = totalSubmissions > 0
                ? ((double) scoredSubmissions / totalSubmissions) * 100.0
                : 0.0;

        return RblScoringProgressResponse.builder()
                .roundId(roundId)
                .totalSubmissions((int) totalSubmissions)
                .scoredSubmissions((int) scoredSubmissions)
                .completionPct(Math.round(completionPct * 100.0) / 100.0)
                .build();
    }
}