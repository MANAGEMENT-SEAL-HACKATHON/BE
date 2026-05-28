package com.sealhackathon.api.rounds.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvanceTeamsRequest {

    @NotEmpty
    private List<Integer> advancedTeamIds;

    @Builder.Default
    private List<Integer> eliminatedTeamIds = List.of();

    private String note;
}
