package com.sealhackathon.api.individual_rankings.repository;

import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IndividualRankingRepository extends JpaRepository<IndividualRanking, Integer> {

    List<IndividualRanking> findByHackathon_IdOrderByRankAsc(Integer hackathonId);

    @Query("""
            SELECT ir FROM IndividualRanking ir
            JOIN FETCH ir.hackathon h
            WHERE ir.user.id = :userId
              AND h.season = com.sealhackathon.api.hackathons.value_object.Season.Fall
              AND h.year = :year
              AND h.status = com.sealhackathon.api.hackathons.value_object.HackathonStatus.FINISHED
              AND ir.isEnabled = true
            ORDER BY ir.rank ASC, h.id ASC
            """)
    List<IndividualRanking> findFallAwardsForUser(
            @Param("userId") Integer userId,
            @Param("year") Integer year);

    @Modifying
    @Transactional
    void deleteByHackathon_Id(Integer hackathonId);
}
