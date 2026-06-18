package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardCandidateResponse {

    // BỔ SUNG TRƯỜNG NÀY ĐỂ FE GỌI API PATCH
    private Integer reviewId;
    private Integer teamId;
    private String teamName;
    private String assignedGroup;
    private Integer candidateRank;
    private Double totalScore;
    private String reason;
    private Boolean coordinatorApproved;
    private String coordinatorNote;
}