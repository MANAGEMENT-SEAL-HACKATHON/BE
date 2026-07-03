package com.sealhackathon.api.mentors.repository;

import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MentorTeamAssignmentRepository extends JpaRepository<MentorTeamAssignment, Integer> {

    Optional<MentorTeamAssignment> findByTeam_IdAndRound_Id(Integer teamId, Integer roundId);

    List<MentorTeamAssignment> findByTeam_IdOrderByRound_IdAsc(Integer teamId);

    boolean existsByTeam_Id(Integer teamId);

    boolean existsByMentor_IdAndTeam_Id(Integer mentorId, Integer teamId);

    List<MentorTeamAssignment> findByMentor_Id(Integer mentorId);

    List<MentorTeamAssignment> findByMentor_IdAndRound_Id(Integer mentorId, Integer roundId);

    List<MentorTeamAssignment> findByHackathon_Id(Integer hackathonId);
}
