package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationQueueResponse {

    private Integer roundId;
    private List<TrackQueueItem> tracks;
    private RoomStats roomStats;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrackQueueItem {
        private Integer trackId;
        private String trackName;
        private Boolean shuffled;
        private List<QueueItem> items;
        /** Chung kết — tổng đội vào vòng (TeamRoundParticipation). */
        private Integer participatingTeamCount;
        /** Chung kết — đội đã nộp bài đủ điều kiện vào queue. */
        private Integer gradableTeamCount;
        /** Chung kết — danh sách đội (coordinator), gồm cả chưa nộp. */
        private List<EligibleTeamItem> eligibleTeams;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EligibleTeamItem {
        private Integer teamId;
        private String teamName;
        private Boolean gradable;
        private String submissionStatus;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueueItem {
        private Integer submissionId;
        private String displayCode;
        private Integer teamId;
        private String teamName;
        private Integer order;
        private String status;
        private String presentationSchedule;
        private String location;
        private PresentationTimerBlock timer;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomStats {
        private int total;
        private int done;
        private int absent;
    }
}
