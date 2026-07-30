package com.sealhackathon.api.showcase.mapper;

import com.sealhackathon.api.showcase.dto.response.HallOfFameEntryResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleBlockResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleSummaryResponse;
import com.sealhackathon.api.showcase.entity.HallOfFameEntry;
import com.sealhackathon.api.showcase.entity.ShowcaseArticle;
import com.sealhackathon.api.showcase.entity.ShowcaseArticleBlock;
import com.sealhackathon.api.showcase.value_object.ShowcaseBlockType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Component
public class ShowcaseMapper {

    public HallOfFameEntryResponse toHofResponse(HallOfFameEntry e) {
        if (e == null) {
            return null;
        }
        return HallOfFameEntryResponse.builder()
                .id(e.getId())
                .hackathonId(e.getHackathonId())
                .hackathonName(e.getHackathonName())
                .year(e.getYear())
                .season(e.getSeason())
                .teamId(e.getTeamId())
                .teamName(e.getTeamName())
                .memberNames(e.getMemberNames())
                .trackName(e.getTrackName())
                .prizeName(e.getPrizeName())
                .prizeValue(e.getPrizeValue())
                .awardedAt(e.getAwardedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public ShowcaseArticleSummaryResponse toSummary(ShowcaseArticle a) {
        if (a == null) {
            return null;
        }
        return ShowcaseArticleSummaryResponse.builder()
                .id(a.getId())
                .hackathonId(a.getHackathonId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .coverUrl(coverUrl(a.getSlug(), a.getCoverImageKey()))
                .status(a.getStatus())
                .publishedAt(a.getPublishedAt())
                .build();
    }

    public ShowcaseArticleResponse toResponse(ShowcaseArticle a) {
        if (a == null) {
            return null;
        }
        List<ShowcaseArticleBlockResponse> blocks = a.getBlocks() == null
                ? List.of()
                : a.getBlocks().stream()
                .sorted(Comparator.comparing(b -> b.getSortOrder() == null ? 0 : b.getSortOrder()))
                .map(b -> toBlockResponse(a.getSlug(), b))
                .toList();

        return ShowcaseArticleResponse.builder()
                .id(a.getId())
                .hackathonId(a.getHackathonId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .coverImageKey(a.getCoverImageKey())
                .coverUrl(coverUrl(a.getSlug(), a.getCoverImageKey()))
                .status(a.getStatus())
                .publishedAt(a.getPublishedAt())
                .authorId(a.getAuthor() != null ? a.getAuthor().getId() : null)
                .authorName(a.getAuthor() != null ? a.getAuthor().getFullName() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .blocks(blocks)
                .build();
    }

    public ShowcaseArticleBlockResponse toBlockResponse(String slug, ShowcaseArticleBlock b) {
        String imageUrl = null;
        if (b.getType() == ShowcaseBlockType.IMAGE && StringUtils.hasText(b.getImageKey()) && b.getId() != null) {
            imageUrl = "/api/v1/public/articles/" + slug + "/blocks/" + b.getId() + "/image";
        }
        return ShowcaseArticleBlockResponse.builder()
                .id(b.getId())
                .sortOrder(b.getSortOrder())
                .type(b.getType())
                .text(b.getText())
                .imageKey(b.getImageKey())
                .imageUrl(imageUrl)
                .build();
    }

    private static String coverUrl(String slug, String coverImageKey) {
        if (!StringUtils.hasText(coverImageKey) || !StringUtils.hasText(slug)) {
            return null;
        }
        return "/api/v1/public/articles/" + slug + "/cover";
    }
}
