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

    private List<GroupItem> groups;
    private RoomStats roomStats;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupItem {
        private String groupName;
        private List<TeamQueueItem> teams;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamQueueItem {
        private Integer teamId;
        private String teamName;
        private Integer order;
        private String status;
        private String presentationSchedule;
        private String location;
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
