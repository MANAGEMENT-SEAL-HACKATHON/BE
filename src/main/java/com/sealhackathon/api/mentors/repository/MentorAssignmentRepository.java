package com.sealhackathon.api.mentors.repository;

import com.sealhackathon.api.mentors.entity.MentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Integer> {

    boolean existsByMentorIdAndTrackId(Integer mentorId, Integer trackId);

    /**
     * Mentor đã được gán bất kỳ bảng nào thuộc cùng vòng Sơ loại.
     * Dùng để chặn 1 người quán xuyến nhiều bảng cùng lúc.
     */
    @Query("""
            SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END
            FROM MentorAssignment ma
            WHERE ma.mentor.id = :mentorId
              AND ma.track.round.id = :roundId
            """)
    boolean existsByMentorIdAndRoundId(@Param("mentorId") Integer mentorId, @Param("roundId") Integer roundId);

    /**
     * Mentor đã gán bảng khác trong cùng vòng (loại trừ track đang gán).
     */
    @Query("""
            SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END
            FROM MentorAssignment ma
            WHERE ma.mentor.id = :mentorId
              AND ma.track.round.id = :roundId
              AND ma.track.id <> :excludeTrackId
            """)
    boolean existsByMentorIdAndRoundIdExcludingTrack(@Param("mentorId") Integer mentorId,
                                                     @Param("roundId") Integer roundId,
                                                     @Param("excludeTrackId") Integer excludeTrackId);

    List<MentorAssignment> findByTrackId(Integer trackId);

    List<MentorAssignment> findByTrack_Round_Hackathon_Id(Integer hackathonId);

    List<MentorAssignment> findByMentorId(Integer mentorId);

    long countByMentorId(Integer mentorId);

    /**
     * Conflict check 2 chiều (FR-05c): với 1 userId, tìm mentor assignment trong track chứa các round
     * mà user này được phân công Judge.
     */
    @Query("""
            SELECT ma FROM MentorAssignment ma
            WHERE ma.mentor.id = :userId
              AND ma.track.id  = :trackId
            """)
    List<MentorAssignment> findByMentorIdAndTrackId(@Param("userId") Integer userId,
                                                    @Param("trackId") Integer trackId);
}
