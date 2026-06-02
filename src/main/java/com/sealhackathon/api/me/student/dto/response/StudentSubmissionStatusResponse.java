package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubmissionStatusResponse {

    private Integer submissionId;
    private Integer roundId;
    private String status;
    private LocalDateTime submittedAt;
}
