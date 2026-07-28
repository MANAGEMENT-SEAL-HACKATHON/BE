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
 * FR-01 POST /api/v1/hackathons
 *
 * <p>Lưu ý: KHÔNG nhận {@code status} — luôn DRAFT khi tạo. Validate logic
 * {@code eventStart >= registrationEnd} thực hiện ở service (vì không phải start/end pair).
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
public class CreateHackathonRequest {

    @NotBlank
    @Size(max = 300)
    private String name;

    @NotBlank
    @Size(max = 150)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "slug chỉ chứa a-z, 0-9 và dấu '-'")
    private String slug;

    @NotNull
    private Season season;

    @NotNull
    @Min(value = 2024, message = "year phải >= 2024")
    private Integer year;

    private String description;

    private String rules;

    private LocalDate registrationStart;

    private LocalDate registrationEnd;

    private LocalDate eventStart;

    private LocalDate eventEnd;

    private Boolean wildcardEnabled;

    private Boolean individualRankingEnabled;

    /**
     * Pending #5 — placeholder; BTC chưa định nghĩa công thức.
     */
    private String chapterScoringFormula;

    @NotNull(message = "Bắt buộc phải dự kiến số lượng người đăng ký tham gia tối đa")
    @Min(value = 1, message = "Số lượng người đăng ký tối đa phải lớn hơn 0")
    private Integer maxParticipants;
}
