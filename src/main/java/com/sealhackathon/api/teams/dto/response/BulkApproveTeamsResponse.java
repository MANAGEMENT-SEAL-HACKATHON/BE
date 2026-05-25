package com.sealhackathon.api.teams.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkApproveTeamsResponse {

    private int approvedCount;
    private List<Integer> approvedTeamIds;
    private List<String> errors;
}
