package com.sealhackathon.api.tracks.repository;

import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    List<Track> findByRoundIdOrderBySequenceOrderAsc(Integer roundId);

    long countByRoundId(Integer roundId);

    @Query("""
            SELECT t FROM Track t
             WHERE t.round.hackathon.id = :hackathonId
             ORDER BY t.id
            """)
    List<Track> findByHackathonIdOrderById(@Param("hackathonId") Integer hackathonId);

    @Query("""
            SELECT t FROM Track t
             WHERE t.round.hackathon.id = :hackathonId
               AND t.status = :status
            """)
    List<Track> findByHackathonIdAndStatus(@Param("hackathonId") Integer hackathonId,
                                          @Param("status") TrackStatus status);

    @Query("""
            SELECT COUNT(t) FROM Track t
             WHERE t.round.hackathon.id = :hackathonId
               AND t.status <> com.sealhackathon.api.tracks.value_object.TrackStatus.CANCELLED
            """)
    long countActiveByHackathonId(@Param("hackathonId") Integer hackathonId);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
              FROM Track t
             WHERE t.round.hackathon.id = :hackathonId
            """)
    boolean existsByHackathonId(@Param("hackathonId") Integer hackathonId);
}
