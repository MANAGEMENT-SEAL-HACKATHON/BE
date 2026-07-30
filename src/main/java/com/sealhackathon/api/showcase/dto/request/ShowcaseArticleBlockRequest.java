package com.sealhackathon.api.showcase.dto.request;

import com.sealhackathon.api.showcase.value_object.ShowcaseBlockType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowcaseArticleBlockRequest {

    private Integer id;

    @NotNull
    private ShowcaseBlockType type;

    private Integer sortOrder;

    private String text;

    private String imageKey;
}
