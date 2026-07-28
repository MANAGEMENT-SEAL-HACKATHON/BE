package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chọn ứng viên vé vớt.
 *
 * <p>Plan C: sort avgScore DESC → submittedAt ASC → teamId ASC, lấy đúng {@code slots} đội.
 */
public final class WildcardCandidateSelection {

    private WildcardCandidateSelection() {
    }

    /** Plan C — đề xuất đúng {@code slots} đội theo thứ tự ổn định. */
    public static List<RoundRankingItemResponse> selectExactSlots(
            List<RoundRankingItemResponse> remainingTeams, int slots) {
        if (slots <= 0 || remainingTeams == null || remainingTeams.isEmpty()) {
            return List.of();
        }
        List<RoundRankingItemResponse> sorted = sortProposalOrder(remainingTeams);
        if (sorted.size() <= slots) {
            return sorted;
        }
        return sorted.subList(0, slots);
    }

    public static List<RoundRankingItemResponse> sortProposalOrder(
            List<RoundRankingItemResponse> teams) {
        List<RoundRankingItemResponse> sorted = new ArrayList<>(teams);
        sorted.sort(proposalComparator());
        return sorted;
    }

    public static Comparator<RoundRankingItemResponse> proposalComparator() {
        return Comparator
                .comparing(RoundRankingItemResponse::getTotalScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RoundRankingItemResponse::getSubmittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RoundRankingItemResponse::getTeamId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    public static Comparator<LocalDateTime> submittedAtAscNullsLast() {
        return Comparator.nullsLast(Comparator.naturalOrder());
    }
}
