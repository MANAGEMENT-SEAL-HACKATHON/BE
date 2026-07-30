package com.sealhackathon.api.showcase.dto.response;

import com.sealhackathon.api.showcase.value_object.ShowcaseBlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ShowcaseArticleBlockResponse {

    private final Integer id;
    private final Integer sortOrder;
    private final ShowcaseBlockType type;
    private final String text;
    private final String imageKey;
    private final String imageUrl;
}
