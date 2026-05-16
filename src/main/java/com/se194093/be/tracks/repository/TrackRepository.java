package com.se194093.be.tracks.repository;

import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.value_object.TrackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    List<Track> findByHackathonIdOrderById(Integer hackathonId);

    List<Track> findByHackathonIdAndStatus(Integer hackathonId, TrackStatus status);

    @Query("SELECT COUNT(t) FROM Track t WHERE t.hackathon.id = :hackathonId AND t.status <> 'CANCELLED'")
    long countActiveByHackathonId(@Param("hackathonId") Integer hackathonId);

    boolean existsByHackathonId(Integer hackathonId);
}
