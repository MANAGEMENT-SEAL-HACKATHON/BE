package com.sealhackathon.api.hackathons.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonLotteryResponse {

    private Integer hackathonId;
    private Integer roundId;
    private int assignedCount;
    /** @deprecated dùng {@link #assignments} — giữ tương thích FE cũ */
    private List<Integer> teamIds;
    private List<AssignmentResult> assignments;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentResult {
        private Integer teamId;
        private Integer trackId;
        private String trackName;
        private String assignedGroup;
    }
}
