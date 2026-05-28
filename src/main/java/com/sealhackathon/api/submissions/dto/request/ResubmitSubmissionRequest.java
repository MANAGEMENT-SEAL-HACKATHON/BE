package com.sealhackathon.api.submissions.dto.request;

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
public class ResubmitSubmissionRequest {

    private String repoUrl;

    private String demoUrl;

    private String reportUrl;

    private String slideUrl;

    private String reason;
}
