package com.sealhackathon.api.mentor_team_assignments.repository;

import com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MentorTeamAssignmentRepository extends JpaRepository<MentorTeamAssignment, Integer> {

    Optional<MentorTeamAssignment> findByTeam_IdAndRound_Id(Integer teamId, Integer roundId);

    List<MentorTeamAssignment> findByTeam_IdOrderByRound_IdAsc(Integer teamId);

    boolean existsByTeam_Id(Integer teamId);
}
