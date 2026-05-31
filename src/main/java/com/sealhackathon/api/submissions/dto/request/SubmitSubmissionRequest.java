package com.sealhackathon.api.submissions.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitSubmissionRequest {

    @NotNull
    private Integer teamId;

    private Integer trackId;

    private Integer roundId;

    private String repoUrl;

    private String demoUrl;

    private String reportUrl;

    private String slideUrl;

    private String lateReason;
}
