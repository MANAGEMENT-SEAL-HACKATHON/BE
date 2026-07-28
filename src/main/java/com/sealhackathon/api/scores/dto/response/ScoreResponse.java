package com.sealhackathon.api.scores.dto.response;

import com.sealhackathon.api.scores.value_object.ScoreType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreResponse {

    private Integer id;
    private Integer submissionId;
    private Integer judgeId;
    private Integer criterionId;
    private Float scoreValue;
    private String comment;
    private ScoreType scoreType;
    private Boolean isFinal;
    private LocalDateTime scoredAt;
    private LocalDateTime updatedAt;
}
