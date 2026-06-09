package com.sealhackathon.api.judge_assignments.repository;

import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Integer> {

    boolean existsByJudgeIdAndRoundId(Integer judgeId, Integer roundId);

    boolean existsByJudgeIdAndTrackId(Integer judgeId, Integer trackId);

    /**
     * Judge gán trực tiếp round CK ({@code ja.round}) hoặc track thuộc round Sơ loại ({@code ja.track}).
     * Phải LEFT JOIN track — path {@code ja.track.round.id} tạo INNER JOIN và loại assignment CK (track_id NULL).
     */
    @Query("""
            SELECT CASE WHEN COUNT(ja) > 0 THEN true ELSE false END
            FROM JudgeAssignment ja
            LEFT JOIN ja.track t
            WHERE ja.judge.id = :judgeId
              AND (ja.round.id = :roundId OR t.round.id = :roundId)
            """)
    boolean existsByJudgeIdAndRoundScope(@Param("judgeId") Integer judgeId, @Param("roundId") Integer roundId);

    List<JudgeAssignment> findByRoundId(Integer roundId);

    List<JudgeAssignment> findByTrackId(Integer trackId);

    Optional<JudgeAssignment> findFirstByTrack_IdAndAssignmentType(
            Integer trackId, JudgeAssignmentType assignmentType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JudgeAssignment ja WHERE ja.round.id = :roundId")
    void deleteByRoundId(@Param("roundId") Integer roundId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JudgeAssignment ja WHERE ja.track.id = :trackId")
    void deleteByTrackId(@Param("trackId") Integer trackId);

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

    @Query("""
            SELECT COUNT(ja) > 0 FROM JudgeAssignment ja
            WHERE ja.judge.id = :judgeId
              AND ja.track IS NOT NULL
              AND ja.track.round.hackathon.id = :hackathonId
              AND ja.track.round.isFinal = FALSE
            """)
    boolean hasPreliminaryTrackAssignmentInHackathon(@Param("judgeId") Integer judgeId,
                                                     @Param("hackathonId") Integer hackathonId);
}
