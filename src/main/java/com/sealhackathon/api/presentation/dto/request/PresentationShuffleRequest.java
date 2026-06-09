package com.sealhackathon.api.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationShuffleRequest {

    private Integer roundId;
    private List<Integer> trackIds;
}
