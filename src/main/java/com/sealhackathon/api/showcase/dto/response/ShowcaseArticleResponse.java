package com.sealhackathon.api.showcase.dto.response;

import com.sealhackathon.api.showcase.value_object.ShowcaseArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ShowcaseArticleResponse {

    private final Integer id;
    private final Integer hackathonId;
    private final String slug;
    private final String title;
    private final String summary;
    private final String coverImageKey;
    private final String coverUrl;
    private final ShowcaseArticleStatus status;
    private final LocalDateTime publishedAt;
    private final Integer authorId;
    private final String authorName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<ShowcaseArticleBlockResponse> blocks;
}
