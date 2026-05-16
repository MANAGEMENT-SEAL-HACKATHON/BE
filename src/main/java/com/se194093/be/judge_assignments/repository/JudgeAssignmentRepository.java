package com.se194093.be.judge_assignments.repository;

import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Integer> {

    boolean existsByJudgeIdAndRoundId(Integer judgeId, Integer roundId);

    List<JudgeAssignment> findByRoundId(Integer roundId);

    List<JudgeAssignment> findByJudgeId(Integer judgeId);

    long countByJudgeId(Integer judgeId);

    /**
     * Conflict check 2 chiều (FR-05b): với 1 mentorId và trackId, tìm các Round trong cùng Track
     * mà user đang được phân công Judge → để cảnh báo Mentor↔Judge overlap.
     */
    @Query("""
            SELECT ja FROM JudgeAssignment ja
            WHERE ja.judge.id = :userId
              AND ja.round.track.id = :trackId
            """)
    List<JudgeAssignment> findByJudgeIdAndRoundTrackId(@Param("userId") Integer userId,
                                                      @Param("trackId") Integer trackId);
}
