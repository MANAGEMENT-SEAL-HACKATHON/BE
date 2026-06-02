package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRankingResponse {

    private Integer hackathonId;
    private List<StudentLeaderboardItemResponse> items;
}
