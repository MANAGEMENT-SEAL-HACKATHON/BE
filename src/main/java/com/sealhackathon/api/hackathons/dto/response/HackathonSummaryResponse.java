package com.sealhackathon.api.hackathons.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Bản tóm tắt cho list endpoint. Bỏ các field text dài (description/rules).
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HackathonSummaryResponse {

    private final Integer id;
    private final String name;
    private final String slug;
    private final Season season;
    private final Integer year;
    private final HackathonStatus status;
    private final LocalDate registrationStart;
    private final LocalDate registrationEnd;
    private final LocalDate eventStart;
    private final LocalDate eventEnd;
    private final Integer maxParticipants;
    private final String bannerUrl;
    private final Integer clonedFromHackathonId;
    private final String clonedFromHackathonName;
    private final java.time.LocalDateTime clonedAt;
}
