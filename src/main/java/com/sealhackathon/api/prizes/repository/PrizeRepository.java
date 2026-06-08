package com.sealhackathon.api.prizes.repository;

import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Integer> {

    List<Prize> findByRound_Hackathon_IdOrderByAwardedAtDesc(Integer hackathonId);

    boolean existsByRound_IdAndTeam_Id(Integer roundId, Integer teamId);

    boolean existsByRound_IdAndPrizeRank(Integer roundId, PrizeRank prizeRank);

    // Hàm lấy danh sách giải thưởng dựa trên danh sách Team ID
    List<Prize> findByTeam_IdIn(List<Integer> teamIds);

    @Query("""
            SELECT COUNT(p) > 0
              FROM Prize p
             WHERE p.round.hackathon.id = :hackathonId
               AND p.team.id = :teamId
            """)
    boolean existsByHackathonIdAndTeamId(
            @Param("hackathonId") Integer hackathonId,
            @Param("teamId") Integer teamId);

    @Query("""
            SELECT COUNT(p) > 0
              FROM Prize p
             WHERE p.round.hackathon.id = :hackathonId
               AND p.prizeRank = :prizeRank
            """)
    boolean existsByHackathonIdAndPrizeRank(
            @Param("hackathonId") Integer hackathonId,
            @Param("prizeRank") PrizeRank prizeRank);
}
