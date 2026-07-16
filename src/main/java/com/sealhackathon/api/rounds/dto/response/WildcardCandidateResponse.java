package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardCandidateResponse {

    private Integer reviewId;
    private Integer teamId;
    private String teamName;
    private String assignedGroup;
    private Integer candidateRank;
    private Double totalScore;
    private LocalDateTime submittedAt;
    private Boolean systemProposed;
    private String reason;
    private Boolean coordinatorApproved;
    private String coordinatorNote;
    private Boolean isOverride;
    private String overrideReasonCategory;
}
