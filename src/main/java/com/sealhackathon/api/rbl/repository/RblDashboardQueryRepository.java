package com.sealhackathon.api.rbl.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Native SQL read-side for RBL dashboard (variance / scoring progress). */
@Repository
public class RblDashboardQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> findPerJudgeSpread(Integer roundId, String scoreType) {
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
        query.setParameter(2, scoreType);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findInterRaterByCriterion(Integer roundId, String scoreType) {
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
        query.setParameter(2, scoreType);
        return query.getResultList();
    }

    public long countDistinctScoredSubmissions(Integer roundId, String scoreType) {
        String sql = """
            SELECT COUNT(DISTINCT s.submission_id)
            FROM scores s
            JOIN submissions sub ON s.submission_id = sub.id
            WHERE sub.round_id = ?1
              AND s.score_type = ?2
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, roundId);
        query.setParameter(2, scoreType);
        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }
}
