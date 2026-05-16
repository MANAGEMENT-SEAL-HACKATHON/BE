package com.se194093.be.mentor_assignments.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MentorAssignmentResponse {

    private final Integer id;
    private final Integer mentorId;
    private final String mentorFullName;
    private final String mentorEmail;
    private final Integer trackId;
    private final String trackName;
    private final LocalDateTime assignedAt;
    private final Integer assignedById;
}
