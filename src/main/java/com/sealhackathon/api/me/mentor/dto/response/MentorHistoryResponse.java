package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorHistoryResponse {

    private List<MentorHistoryItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MentorHistoryItem {
        private Integer hackathonId;
        private String hackathonName;
        private Integer teamsMentored;
    }
}
