package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentHistoryResponse {

    private List<StudentHistoryHackathonItem> hackathons;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentHistoryHackathonItem {
        private Integer hackathonId;
        private String name;
        private String role;
        private String outcome;
    }
}
