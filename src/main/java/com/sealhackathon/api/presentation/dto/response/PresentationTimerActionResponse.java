package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationTimerActionResponse {

    private Integer roundId;
    private Integer trackId;
    private Integer submissionId;
    private PresentationTimerBlock timer;
}
