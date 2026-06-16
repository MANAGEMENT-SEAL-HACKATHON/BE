package com.sealhackathon.api.chapter_rankings.repository;

import com.sealhackathon.api.chapter_rankings.entity.ChapterRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChapterRankingRepository extends JpaRepository<ChapterRanking, Integer> {

    List<ChapterRanking> findByHackathon_IdOrderByRankAsc(Integer hackathonId);

    @Modifying
    @Transactional
    void deleteByHackathon_Id(Integer hackathonId);
}
