package com.sealhackathon.api.mentor_assignments.repository;

import com.sealhackathon.api.mentor_assignments.entity.MentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Integer> {

    boolean existsByMentorIdAndTrackId(Integer mentorId, Integer trackId);

    List<MentorAssignment> findByTrackId(Integer trackId);

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
