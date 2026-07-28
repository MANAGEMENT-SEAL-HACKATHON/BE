package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualAwardResponse {

    private Integer hackathonId;
    private String hackathonName;
    private Integer year;
    private Integer rank;
    private String awardName;
    private String category;
}
