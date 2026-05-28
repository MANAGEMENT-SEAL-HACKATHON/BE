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
public class ResolveTiebreakRequest {

    @NotEmpty
    private List<Integer> orderedTeamIds;

    private String note;
}
