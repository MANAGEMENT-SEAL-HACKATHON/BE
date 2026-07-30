package com.sealhackathon.api.hackathons.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationExtensionRequest {

    @NotNull
    private LocalDate newRegistrationEnd;

    /** true → cascade lịch thi qua CompetitionScheduleAdjustService (1 lần). */
    @Builder.Default
    private boolean adjustCompetitionSchedule = false;

    /** Bắt buộc khi adjustCompetitionSchedule=true. */
    private LocalDateTime newPrelimExamAt;

    private CompetitionScheduleOverrides overrides;
}
