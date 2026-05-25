package com.sealhackathon.api.hackathons.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * FR-13B PATCH /api/v1/hackathons/{id}/lottery — batch bốc thăm Track.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonLotteryRequest {

    @NotNull
    private Integer roundId;

    @NotEmpty
    @Valid
    private List<Assignment> assignments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Assignment {

        @NotNull
        private Integer teamId;

        @NotNull
        private Integer trackId;

        private String assignedGroup;
    }
}
