package com.sealhackathon.api.me.student.dto.request;

import com.sealhackathon.api.appeals.dto.request.AppealEvidenceItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateAppealRequest {

    @NotNull
    private Integer teamId;

    @NotNull
    private Integer roundId;

    @NotBlank
    private String reason;

    /** Legacy single URL — still accepted; prefer {@link #evidences}. */
    private String evidenceUrl;

    @Valid
    private List<AppealEvidenceItemRequest> evidences;
}
