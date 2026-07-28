package com.sealhackathon.api.rounds.dto.response;

import com.sealhackathon.api.common.response.Warning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** FR-20 preview — ranking items kèm warnings mềm (incomplete scoring). */
@Getter
@Builder
@AllArgsConstructor
public class RankingPreviewResult {

    private List<RoundRankingItemResponse> items;
    private List<Warning> warnings;
}
