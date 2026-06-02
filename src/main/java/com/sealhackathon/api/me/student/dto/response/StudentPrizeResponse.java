package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPrizeResponse {

    private Integer prizeId;
    private Integer hackathonId;
    private String prizeName;
    private Integer rank;
}
