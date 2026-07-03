package com.sealhackathon.api.mentors.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-05b POST /api/v1/mentor-assignments
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMentorAssignmentRequest {

    @NotNull
    private Integer mentorId;

    @NotNull
    private Integer trackId;
}
