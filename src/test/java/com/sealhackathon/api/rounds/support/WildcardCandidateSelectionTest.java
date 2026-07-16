package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Plan C — exact slots: avgScore DESC, submittedAt ASC, teamId ASC. */
class WildcardCandidateSelectionTest {

    @Test
    void selectsExactSlots_noTieExpansion() {
        List<RoundRankingItemResponse> remaining = List.of(
                item(1, 7.0, "2026-01-01T10:00:00"),
                item(2, 7.0, "2026-01-01T09:00:00"),
                item(3, 7.0, "2026-01-01T11:00:00"),
                item(4, 7.0, "2026-01-01T08:00:00"));

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectExactSlots(remaining, 2);

        // Same score → earlier submittedAt wins → team 4 then 2
        assertThat(selected).extracting(RoundRankingItemResponse::getTeamId)
                .containsExactly(4, 2);
    }

    @Test
    void selectsTopSlotsByScoreThenSubmittedAt() {
        List<RoundRankingItemResponse> remaining = List.of(
                item(1, 8.0, "2026-01-01T12:00:00"),
                item(2, 7.5, "2026-01-01T10:00:00"),
                item(3, 6.0, "2026-01-01T09:00:00"));

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectExactSlots(remaining, 2);

        assertThat(selected).extracting(RoundRankingItemResponse::getTeamId)
                .containsExactly(1, 2);
    }

    @Test
    void teamIdBreaksSubmittedAtTie() {
        LocalDateTime same = LocalDateTime.parse("2026-01-01T10:00:00");
        List<RoundRankingItemResponse> remaining = List.of(
                RoundRankingItemResponse.builder().teamId(5).totalScore(7.0).submittedAt(same).build(),
                RoundRankingItemResponse.builder().teamId(3).totalScore(7.0).submittedAt(same).build(),
                RoundRankingItemResponse.builder().teamId(9).totalScore(7.0).submittedAt(same).build());

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectExactSlots(remaining, 2);

        assertThat(selected).extracting(RoundRankingItemResponse::getTeamId)
                .containsExactly(3, 5);
    }

    @Test
    void returnsAllWhenPoolSmallerThanSlots() {
        List<RoundRankingItemResponse> remaining = List.of(
                item(1, 8.0, "2026-01-01T10:00:00"),
                item(2, 7.0, "2026-01-01T09:00:00"));

        assertThat(WildcardCandidateSelection.selectExactSlots(remaining, 5))
                .hasSize(2);
    }

    private static RoundRankingItemResponse item(int teamId, double score, String submittedAt) {
        return RoundRankingItemResponse.builder()
                .teamId(teamId)
                .totalScore(score)
                .submittedAt(LocalDateTime.parse(submittedAt))
                .build();
    }
}
