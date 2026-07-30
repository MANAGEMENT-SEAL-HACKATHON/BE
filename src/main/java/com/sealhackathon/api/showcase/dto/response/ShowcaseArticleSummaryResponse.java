package com.sealhackathon.api.showcase.dto.response;

import com.sealhackathon.api.showcase.value_object.ShowcaseArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ShowcaseArticleSummaryResponse {

    private final Integer id;
    private final Integer hackathonId;
    private final String slug;
    private final String title;
    private final String summary;
    private final String coverUrl;
    private final ShowcaseArticleStatus status;
    private final LocalDateTime publishedAt;
}
