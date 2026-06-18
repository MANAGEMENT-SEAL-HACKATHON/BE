package com.sealhackathon.api.wildcard_reviews.repository;

import com.sealhackathon.api.wildcard_reviews.entity.WildcardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WildcardReviewRepository extends JpaRepository<WildcardReview, Integer> {

    List<WildcardReview> findByRound_Id(Integer roundId);

    Optional<WildcardReview> findByRound_IdAndTeam_Id(Integer roundId, Integer teamId);

    List<WildcardReview> findByRound_IdAndCoordinatorApprovedIsNull(Integer roundId);

    long countByRound_IdAndCoordinatorApproved(Integer roundId, Boolean coordinatorApproved);
}
