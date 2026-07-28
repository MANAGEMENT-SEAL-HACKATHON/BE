package com.sealhackathon.api.teams.repository;

import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Team t WHERE t.id = :id")
    Optional<Team> findByIdForUpdate(@Param("id") Integer id);

    List<Team> findByHackathon_Id(Integer hackathonId);

    List<Team> findByHackathon_IdAndStatus(Integer hackathonId, TeamStatus status);

    List<Team> findByLeader_Id(Integer leaderId);

    List<Team> findByStatusAndFormationSubmittedAtIsNullAndFormationGraceDeadlineAtIsNotNullAndFormationGraceDeadlineAtBefore(
            TeamStatus status, LocalDateTime deadline);

    boolean existsByHackathon_IdAndTeamNameIgnoreCase(Integer hackathonId, String teamName);

    Optional<Team> findByHackathon_IdAndTeamNameIgnoreCase(Integer hackathonId, String teamName);

    long countByHackathon_IdAndStatusIn(Integer hackathonId, Collection<TeamStatus> statuses);

    @Query("""
            SELECT COUNT(DISTINCT t.id) FROM Team t
            JOIN TeamRoundTrack trt ON trt.team = t
            WHERE trt.track.id = :trackId AND t.status IN :statuses
            """)
    long countActiveByTrackId(
            @Param("trackId") Integer trackId,
            @Param("statuses") Collection<TeamStatus> statuses);

    Optional<Team> findByIdAndLeader_Id(Integer teamId, Integer leaderId);
}
