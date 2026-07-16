package com.sealhackathon.api.wildcard_reviews.repository;

import com.sealhackathon.api.wildcard_reviews.entity.WildcardOverrideHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WildcardOverrideHistoryRepository extends JpaRepository<WildcardOverrideHistory, Integer> {

    List<WildcardOverrideHistory> findByRound_IdOrderByOverriddenAtDesc(Integer roundId);
}
