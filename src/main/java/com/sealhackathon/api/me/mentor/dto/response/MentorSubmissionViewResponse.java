package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorSubmissionViewResponse {

    private Integer submissionId;
    private Integer roundId;
    private String status;
    private LocalDateTime submittedAt;
}
