package com.sealhackathon.api.rounds.dto.response;

import com.sealhackathon.api.common.response.Warning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** FR-20A lock — round summary kèm warnings mềm. */
@Getter
@Builder
@AllArgsConstructor
public class LockScoringResult {

    private RoundSummaryResponse round;
    private List<Warning> warnings;
}
