package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chọn ứng viên vé vớt — gồm mọi đội đồng điểm tại ngưỡng cutoff. */
public final class WildcardCandidateSelection {

    private WildcardCandidateSelection() {
    }

    public static List<RoundRankingItemResponse> selectWithTiesAtCutoff(
            List<RoundRankingItemResponse> remainingTeams, int slots) {
        if (slots <= 0 || remainingTeams == null || remainingTeams.isEmpty()) {
            return List.of();
        }
        List<RoundRankingItemResponse> sorted = new ArrayList<>(remainingTeams);
        sorted.sort(Comparator
                .comparing(RoundRankingItemResponse::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RoundRankingItemResponse::getTeamId, Comparator.nullsLast(Comparator.naturalOrder())));

        if (sorted.size() <= slots) {
            return sorted;
        }

        Double cutoffScore = sorted.get(slots - 1).getTotalScore();
        if (cutoffScore == null) {
            return sorted.stream().limit(slots).toList();
        }

        List<RoundRankingItemResponse> selected = new ArrayList<>();
        for (RoundRankingItemResponse item : sorted) {
            if (item.getTotalScore() == null || item.getTotalScore() + 1e-9 < cutoffScore) {
                break;
            }
            selected.add(item);
        }
        return selected;
    }
}
