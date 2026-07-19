package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RoundRankingQueryServiceTest {

    @Test
    void sortRankRows_putsEliminatedTeamsLastWithinGroup() {
        List<RankRow> rows = List.of(
                row(5, "GD3-05", 6.10, ParticipationStatus.ELIMINATED),
                row(3, "GD3-03", 5.60, ParticipationStatus.PARTICIPATING),
                row(1, "GD3-01", 3.10, ParticipationStatus.PARTICIPATING));

        List<RankRow> sorted = RoundRankingQueryService.sortRankRows(rows, false);

        assertThat(sorted).extracting(RankRow::teamName)
                .containsExactly("GD3-03", "GD3-01", "GD3-05");
    }

    @Test
    void assignRanks_renumbersAfterEliminatedTeamMovedToBottom() {
        List<RankRow> sorted = List.of(
                row(3, "GD3-03", 5.60, ParticipationStatus.PARTICIPATING),
                row(1, "GD3-01", 3.10, ParticipationStatus.PARTICIPATING),
                row(5, "GD3-05", 6.10, ParticipationStatus.ELIMINATED));

        List<RoundRankingItemResponse> ranked =
                RoundRankingQueryService.assignRanks(sorted, false);

        assertThat(ranked).extracting(RoundRankingItemResponse::getTeamName, RoundRankingItemResponse::getRank)
                .containsExactly(
                        tuple("GD3-03", 1),
                        tuple("GD3-01", 2),
                        tuple("GD3-05", 3));
        assertThat(ranked.get(2).getParticipationStatus())
                .isEqualTo(ParticipationStatus.ELIMINATED.name());
    }

    @Test
    void sortRankRows_respectsAssignedGroupBeforeElimination() {
        List<RankRow> rows = List.of(
                row(10, "B2-Elim", 9.0, ParticipationStatus.ELIMINATED, "BANG-2"),
                row(11, "B2-Active", 5.0, ParticipationStatus.PARTICIPATING, "BANG-2"),
                row(20, "B1-Elim", 8.0, ParticipationStatus.ELIMINATED, "BANG-1"),
                row(21, "B1-Active", 4.0, ParticipationStatus.PARTICIPATING, "BANG-1"));

        List<RankRow> sorted = RoundRankingQueryService.sortRankRows(rows, false);

        assertThat(sorted).extracting(RankRow::assignedGroup, RankRow::teamName)
                .containsExactly(
                        tuple("BANG-1", "B1-Active"),
                        tuple("BANG-1", "B1-Elim"),
                        tuple("BANG-2", "B2-Active"),
                        tuple("BANG-2", "B2-Elim"));
    }

    @Test
    void assignRanks_flagsTiedScoresWithinGroup() {
        List<RankRow> sorted = List.of(
                row(3, "GD3-03", 5.60, ParticipationStatus.PARTICIPATING),
                row(1, "GD3-01", 5.60, ParticipationStatus.PARTICIPATING),
                row(5, "GD3-05", 3.10, ParticipationStatus.PARTICIPATING));

        List<RoundRankingItemResponse> ranked =
                RoundRankingQueryService.assignRanks(sorted, false);

        assertThat(ranked).extracting(RoundRankingItemResponse::getTeamName, RoundRankingItemResponse::getTiebreakRequired)
                .containsExactly(
                        tuple("GD3-03", true),
                        tuple("GD3-01", true),
                        tuple("GD3-05", false));
    }

    @Test
    void assignRanks_clearsTieFlagWhenMicroPenaltyDiffersEffectiveScore() {
        // Cùng điểm gốc 8.50 nhưng T02 đã bị micro-penalty → không còn đồng điểm hiệu lực
        List<RankRow> sorted = List.of(
                rowWithPenalty(1, "T01", 8.50, 0.0),
                rowWithPenalty(2, "T02", 8.50, 0.01));

        List<RoundRankingItemResponse> ranked =
                RoundRankingQueryService.assignRanks(sorted, false, false);

        assertThat(ranked).extracting(RoundRankingItemResponse::getTeamName, RoundRankingItemResponse::getTiebreakRequired)
                .containsExactly(
                        tuple("T01", false),
                        tuple("T02", false));
    }

    private static RankRow row(int teamId, String name, double score, ParticipationStatus status) {
        return row(teamId, name, score, status, "BANG-1");
    }

    private static RankRow row(
            int teamId, String name, double score, ParticipationStatus status, String group) {
        return new RankRow(null, teamId, name, 1, group, score, status.name(), null, null, 0.0);
    }

    private static RankRow rowWithPenalty(int teamId, String name, double score, double penalty) {
        return new RankRow(
                null, teamId, name, 1, "BANG-1", score,
                ParticipationStatus.PARTICIPATING.name(), null, null, penalty);
    }
}
