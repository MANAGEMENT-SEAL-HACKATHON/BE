package com.sealhackathon.api.showcase.dto.response;

import com.sealhackathon.api.hackathons.value_object.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class HallOfFameEntryResponse {

    private final Integer id;
    private final Integer hackathonId;
    private final String hackathonName;
    private final Integer year;
    private final Season season;
    private final Integer teamId;
    private final String teamName;
    private final String memberNames;
    private final String trackName;
    private final String prizeName;
    private final String prizeValue;
    private final String prizeDescription;
    private final LocalDateTime awardedAt;
    private final LocalDateTime createdAt;

    /** Enrichment from published ShowcaseArticle (optional). */
    private final String articleSlug;
    private final String articleTitle;
    private final String articleSummary;
    private final String coverUrl;
    private final String hackathonBannerUrl;
}
