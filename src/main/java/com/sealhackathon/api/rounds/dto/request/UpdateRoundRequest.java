package com.sealhackathon.api.rounds.dto.request;

import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * FR-03 PUT /api/v1/rounds/{id}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoundRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private LocalDateTime examAt;

    private LocalDateTime submissionOpen;

    @NotNull
    private LocalDateTime submissionDeadline;

    @Min(0)
    private Integer codingDurationHours;

    private String problemStatementUrl;

    private LocalDateTime problemReleasedAt;

    @Min(1)
    private Integer topNAdvance;

    private Boolean wildcardEnabled;

    @Min(1)
    private Integer minTeamsFinal;

    private TiebreakRule tiebreakRule;

    private Boolean scoringLocked;

    private Boolean forceLocked;

    private String forceLockReason;
}
