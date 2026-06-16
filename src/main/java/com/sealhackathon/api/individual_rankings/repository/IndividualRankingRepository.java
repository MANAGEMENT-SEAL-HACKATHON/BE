package com.sealhackathon.api.individual_rankings.repository;

import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IndividualRankingRepository extends JpaRepository<IndividualRanking, Integer> {

    List<IndividualRanking> findByHackathon_IdOrderByRankAsc(Integer hackathonId);

    @Modifying
    @Transactional
    void deleteByHackathon_Id(Integer hackathonId);
}
