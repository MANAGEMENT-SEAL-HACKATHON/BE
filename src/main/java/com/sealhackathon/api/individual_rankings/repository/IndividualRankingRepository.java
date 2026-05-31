package com.sealhackathon.api.individual_rankings.repository;

import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndividualRankingRepository extends JpaRepository<IndividualRanking, Integer> {

    List<IndividualRanking> findByHackathon_IdOrderByRankAsc(Integer hackathonId);
}
