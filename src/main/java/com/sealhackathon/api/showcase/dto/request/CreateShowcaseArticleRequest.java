package com.sealhackathon.api.showcase.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateShowcaseArticleRequest {

    @NotBlank
    @Size(max = 180)
    private String slug;

    @NotBlank
    @Size(max = 300)
    private String title;

    private String summary;

    @Valid
    private List<ShowcaseArticleBlockRequest> blocks;
}
