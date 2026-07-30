package com.sealhackathon.api.showcase.service;

import com.sealhackathon.api.showcase.dto.request.CreateShowcaseArticleRequest;
import com.sealhackathon.api.showcase.dto.request.UpdateShowcaseArticleRequest;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleSummaryResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ShowcaseArticleService {

    List<ShowcaseArticleSummaryResponse> listPublished();

    ShowcaseArticleResponse getPublishedBySlug(String slug);

    List<ShowcaseArticleResponse> listByHackathon(Integer hackathonId);

    ShowcaseArticleResponse getById(Integer id);

    ShowcaseArticleResponse create(Integer hackathonId, CreateShowcaseArticleRequest req);

    ShowcaseArticleResponse update(Integer id, UpdateShowcaseArticleRequest req);

    void delete(Integer id);

    ShowcaseArticleResponse publish(Integer id);

    ShowcaseArticleResponse unpublish(Integer id);

    ShowcaseArticleResponse generateDraftFromChampions(Integer hackathonId);

    ShowcaseArticleResponse uploadCover(Integer id, MultipartFile file);

    Map<String, String> uploadBlockImage(Integer id, MultipartFile file);

    Resource loadCoverBySlug(String slug);

    Resource loadBlockImageBySlug(String slug, Integer blockId);
}
