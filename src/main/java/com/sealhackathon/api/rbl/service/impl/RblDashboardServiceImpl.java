package com.sealhackathon.api.rbl.service.impl;

import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.rbl.dto.response.RblInterRaterCriterionResponse;
import com.sealhackathon.api.rbl.dto.response.RblScoringProgressResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rbl.support.JudgeResearchType;
import com.sealhackathon.api.rbl.support.JudgeResearchTypeResolver;
import com.sealhackathon.api.rbl.support.RblJudgeAnonymizer;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RblDashboardServiceImpl implements RblDashboardService {

    private final SubmissionRepository submissionRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RblVarianceResponse varianceByRound(Integer roundId) {
        var round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        Integer hackathonId = round.getHackathon() != null ? round.getHackathon().getId() : null;
        List<RblVarianceItemResponse> perJudge = queryPerJudgeSpread(roundId, hackathonId);
        List<RblInterRaterCriterionResponse> interRater = queryInterRaterByCriterion(roundId);
        return RblVarianceResponse.builder()
                .perJudgeSpread(perJudge)
                .interRaterByCriterion(interRater)
                .build();
    }

    private List<RblVarianceItemResponse> queryPerJudgeSpread(Integer roundId, Integer hackathonId) {
        String sql = """
            SELECT
                c.id as criterionId,
                c.name as criterionName,
                c.type as criterionType,
                s.judge_id as judgeId,
                AVG(s.score_value) as meanScore,
                STDDEV(s.score_value) as stdDev
            FROM scores s
            JOIN criteria c ON s.criterion_id = c.id
            JOIN submissions sub ON s.submission_id = sub.id
            WHERE sub.round_id = ?1
              AND s.score_type = ?2
              AND c.type <> 'PENALTY'
            GROUP BY c.id, c.name, c.type, s.judge_id
            ORDER BY c.id ASC, stdDev DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, roundId);
        query.setParameter(2, ScoreType.NORMAL.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<RblVarianceItemResponse> responses = new ArrayList<>();
        Map<Integer, JudgeResearchType> typeCache = new HashMap<>();

        for (Object[] row : results) {
            Integer judgeId = (Integer) row[3];
            JudgeResearchType researchType = typeCache.computeIfAbsent(judgeId, id -> {
                User u = userRepository.findById(id).orElse(null);
                return JudgeResearchTypeResolver.resolve(u);
            });
            responses.add(RblVarianceItemResponse.builder()
                    .criterionId((Integer) row[0])
                    .criterionName((String) row[1])
                    .criterionType((String) row[2])
                    .anonymizedJudgeId(RblJudgeAnonymizer.anonymize(hackathonId, judgeId))
                    .judgeType(researchType.name())
                    .meanScore(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                    .stdDev(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                    .build());
        }
        return responses;
    }

    /**
     * STDDEV of scores across judges on the same (submission, criterion), then mean by criterion.
     * PENALTY criteria excluded.
     */
    private List<RblInterRaterCriterionResponse> queryInterRaterByCriterion(Integer roundId) {
        String sql = """
            SELECT
                criterion_id,
                criterion_name,
                criterion_type,
                AVG(inter_stddev) AS mean_inter_rater_std,
                COUNT(*) AS submission_count
            FROM (
                SELECT
                    c.id AS criterion_id,
                    c.name AS criterion_name,
                    c.type AS criterion_type,
                    s.submission_id,
                    STDDEV(s.score_value) AS inter_stddev
                FROM scores s
                JOIN criteria c ON s.criterion_id = c.id
                JOIN submissions sub ON s.submission_id = sub.id
                WHERE sub.round_id = ?1
                  AND s.score_type = ?2
                  AND c.type <> 'PENALTY'
                GROUP BY c.id, c.name, c.type, s.submission_id
                HAVING COUNT(DISTINCT s.judge_id) >= 2
            ) per_sub
            GROUP BY criterion_id, criterion_name, criterion_type
            ORDER BY criterion_id ASC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, roundId);
        query.setParameter(2, ScoreType.NORMAL.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<RblInterRaterCriterionResponse> responses = new ArrayList<>();
        for (Object[] row : results) {
            responses.add(RblInterRaterCriterionResponse.builder()
                    .criterionId((Integer) row[0])
                    .criterionName((String) row[1])
                    .criterionType((String) row[2])
                    .meanInterRaterStdDev(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                    .submissionCount(row[4] != null ? ((Number) row[4]).intValue() : 0)
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
