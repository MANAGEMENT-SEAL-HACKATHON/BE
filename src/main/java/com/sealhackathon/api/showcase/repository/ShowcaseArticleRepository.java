package com.sealhackathon.api.showcase.repository;

import com.sealhackathon.api.showcase.entity.ShowcaseArticle;
import com.sealhackathon.api.showcase.value_object.ShowcaseArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowcaseArticleRepository extends JpaRepository<ShowcaseArticle, Integer> {

    Optional<ShowcaseArticle> findBySlug(String slug);

    Optional<ShowcaseArticle> findBySlugAndStatus(String slug, ShowcaseArticleStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Integer id);

    List<ShowcaseArticle> findByStatusOrderByPublishedAtDesc(ShowcaseArticleStatus status);

    List<ShowcaseArticle> findByHackathonIdOrderByUpdatedAtDesc(Integer hackathonId);
}
