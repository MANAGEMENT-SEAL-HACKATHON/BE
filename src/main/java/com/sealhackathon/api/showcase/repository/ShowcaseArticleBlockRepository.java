package com.sealhackathon.api.showcase.repository;

import com.sealhackathon.api.showcase.entity.ShowcaseArticleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowcaseArticleBlockRepository extends JpaRepository<ShowcaseArticleBlock, Integer> {

    List<ShowcaseArticleBlock> findByArticle_IdOrderBySortOrderAsc(Integer articleId);

    Optional<ShowcaseArticleBlock> findByIdAndArticle_Id(Integer id, Integer articleId);

    void deleteByArticle_Id(Integer articleId);
}
