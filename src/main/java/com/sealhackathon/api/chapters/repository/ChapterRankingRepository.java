package com.sealhackathon.api.chapters.repository;

import com.sealhackathon.api.chapters.entity.ChapterRanking;
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
