package com.sealhackathon.api.submissions.dto.request;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
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
public class ReviewSubmissionRequest {

    @NotNull
    private SubmissionStatus status;

    private String reviewNote;
}
