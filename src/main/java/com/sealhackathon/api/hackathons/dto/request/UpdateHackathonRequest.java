package com.sealhackathon.api.hackathons.dto.request;

import com.sealhackathon.api.common.validation.DateRange;
import com.sealhackathon.api.hackathons.value_object.Season;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * FR-01 PUT /api/v1/hackathons/{id}
 *
 * <p>Chỉ cho phép sửa khi {@code status = DRAFT}. KHÔNG cho sửa status qua endpoint này
 * (dùng PATCH /status — FR-06). Service tự kiểm tra status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DateRange(start = "registrationStart", end = "registrationEnd",
        message = "registrationEnd phải >= registrationStart")
@DateRange(start = "eventStart", end = "eventEnd",
        message = "eventEnd phải >= eventStart")
public class UpdateHackathonRequest {

    @NotBlank
    @Size(max = 300)
    private String name;

    @NotBlank
    @Size(max = 150)
    @Pattern(regexp = "^[a-z0-9-]+$")
    private String slug;

    @NotNull
    private Season season;

    @NotNull
    @Min(2024)
    private Integer year;

    private String description;

    private String rules;

    private LocalDate registrationStart;

    private LocalDate registrationEnd;

    private LocalDate eventStart;

    private LocalDate eventEnd;

    private Boolean individualRankingEnabled;

    private String chapterScoringFormula;

    @Min(value = 1, message = "Số lượng người đăng ký tối đa phải lớn hơn 0")
    private Integer maxParticipants;
}
