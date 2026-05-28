package com.sealhackathon.api.team_members.repository;

import com.sealhackathon.api.team_members.entity.TeamMember;
import com.sealhackathon.api.team_members.entity.TeamMemberId;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    List<TeamMember> findByTeam_Id(Integer teamId);

    @Query("""
            SELECT COUNT(tm) > 0 FROM TeamMember tm
            JOIN tm.team t
            WHERE tm.user.id = :userId AND t.hackathon.id = :hackathonId
              AND t.status IN :teamStatuses AND tm.status = :memberStatus
            """)
    boolean existsAcceptedInActiveOrPendingTeam(
            @Param("userId") Integer userId,
            @Param("hackathonId") Integer hackathonId,
            @Param("teamStatuses") Collection<TeamStatus> teamStatuses,
            @Param("memberStatus") TeamMemberStatus memberStatus);

    long countByTeam_IdAndStatus(Integer teamId, TeamMemberStatus status);

    boolean existsByUser_IdAndTeam_IdAndStatus(Integer userId, Integer teamId, TeamMemberStatus status);
}
