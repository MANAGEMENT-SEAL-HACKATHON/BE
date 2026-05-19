package com.se194093.be.judge_assignments.repository;

import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import com.se194093.be.judge_assignments.value_object.JudgeAssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Integer> {

    boolean existsByJudgeIdAndRoundId(Integer judgeId, Integer roundId);

    boolean existsByJudgeIdAndTrackId(Integer judgeId, Integer trackId);

    List<JudgeAssignment> findByRoundId(Integer roundId);

    List<JudgeAssignment> findByTrackId(Integer trackId);

    List<JudgeAssignment> findByJudgeId(Integer judgeId);

    long countByJudgeId(Integer judgeId);

    @Query("""
            SELECT ja FROM JudgeAssignment ja
            WHERE ja.judge.id = :userId
              AND ja.track.id = :trackId
            """)
    List<JudgeAssignment> findByJudgeIdAndTrackId(@Param("userId") Integer userId,
                                                  @Param("trackId") Integer trackId);

    /** @deprecated alias */
    @Deprecated
    default List<JudgeAssignment> findByJudgeIdAndRoundTrackId(Integer userId, Integer trackId) {
        return findByJudgeIdAndTrackId(userId, trackId);
    }

    /**
     * Rule 2 (FR-05): user đã là Judge Chung kết (FINAL_EXTERNAL) trong cùng Hackathon với track.
     */
    @Query("""
            SELECT CASE WHEN COUNT(ja) > 0 THEN true ELSE false END
            FROM JudgeAssignment ja, Track t
            WHERE t.id = :trackId
              AND ja.judge.id = :judgeId
              AND ja.assignmentType = :finalExternal
              AND ja.round IS NOT NULL
              AND ja.round.hackathon.id = t.round.hackathon.id
            """)
    boolean existsFinalExternalJudgeInHackathonOfTrack(@Param("judgeId") Integer judgeId,
                                                     @Param("trackId") Integer trackId,
                                                     @Param("finalExternal") JudgeAssignmentType finalExternal);
}
