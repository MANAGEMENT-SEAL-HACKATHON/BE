package com.sealhackathon.api.submissions.dto.response;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {

    private Integer id;
    private Integer teamId;
    private Integer trackId;
    private Integer roundId;
    private String repoUrl;
    private String demoUrl;
    private String reportUrl;
    private String slideUrl;
    private SubmissionStatus status;
    private Boolean isLate;
    private String lateReason;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime submittedAt;
}
