package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WildcardCandidateSelectionTest {

    @Test
    void includesAllTeamsTiedAtCutoff() {
        List<RoundRankingItemResponse> remaining = List.of(
                item(1, 7.0),
                item(2, 7.0),
                item(3, 7.0),
                item(4, 7.0));

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectWithTiesAtCutoff(remaining, 2);

        assertThat(selected).extracting(RoundRankingItemResponse::getTeamId)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    void selectsTopSlotsWhenNoTieAtCutoff() {
        List<RoundRankingItemResponse> remaining = List.of(
                item(1, 8.0),
                item(2, 7.5),
                item(3, 6.0));

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectWithTiesAtCutoff(remaining, 2);

        assertThat(selected).extracting(RoundRankingItemResponse::getTeamId)
                .containsExactly(1, 2);
    }

    @Test
    void includesTieOnlyAtBoundary() {
        List<RoundRankingItemResponse> remaining = List.of(
                item(1, 8.0),
                item(2, 7.0),
                item(3, 7.0),
                item(4, 6.0));

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectWithTiesAtCutoff(remaining, 2);

        assertThat(selected).extracting(RoundRankingItemResponse::getTeamId)
                .containsExactly(1, 2, 3);
    }

    private static RoundRankingItemResponse item(int teamId, double score) {
        return RoundRankingItemResponse.builder()
                .teamId(teamId)
                .totalScore(score)
                .build();
    }
}
