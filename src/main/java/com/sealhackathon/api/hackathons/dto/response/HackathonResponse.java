package com.sealhackathon.api.hackathons.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response chi tiết của 1 Hackathon. Dùng cho GET {id} / POST / PUT.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HackathonResponse {

    private final Integer id;
    private final String name;
    private final String slug;
    private final Season season;
    private final Integer year;
    private final HackathonStatus status;
    private final String description;
    private final String rules;
    private final String bannerUrl;
    private final LocalDate registrationStart;
    private final LocalDate registrationEnd;
    private final LocalDateTime registrationClosedEarlyAt;
    private final Integer registrationExtensionCount;
    private final LocalDateTime registrationExtendedAt;
    /** Đã dời lịch thi 1 lần (null = chưa). */
    private final LocalDateTime scheduleAdjustedAt;
    private final LocalDate eventStart;
    private final LocalDate eventEnd;
    private final Boolean individualRankingEnabled;
    private final String chapterScoringFormula;
    private final Integer createdById;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Integer maxParticipants;
    private final Integer appealWindowMinutes;
    private final Integer clonedFromHackathonId;
    private final String clonedFromHackathonName;
    private final LocalDateTime clonedAt;
}
