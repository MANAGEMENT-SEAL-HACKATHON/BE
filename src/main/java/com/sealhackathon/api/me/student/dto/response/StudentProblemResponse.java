package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProblemResponse {

    private Integer roundId;
    private String problemStatement;
    private String problemUrl;
    private Boolean released;
}
