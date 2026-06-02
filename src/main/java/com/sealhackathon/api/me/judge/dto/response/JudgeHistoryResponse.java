package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeHistoryResponse {

    private List<JudgeHistoryItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JudgeHistoryItem {
        private Integer hackathonId;
        private String hackathonName;
        private Integer roundId;
        private String roundName;
    }
}
