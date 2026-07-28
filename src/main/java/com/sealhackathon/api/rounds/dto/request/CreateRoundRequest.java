package com.sealhackathon.api.rounds.dto.request;

import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import jakarta.validation.constraints.Max;
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
 * FR-02 POST /api/v1/hackathons/{hackathonId}/rounds
 *
 * <p>Thứ tự vòng: {@link #examAt} (ngày giờ thi), không dùng sequenceOrder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoundRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /** Ngày giờ thi — bắt buộc; phải trước {@code submissionOpen}. */
    @NotNull
    private LocalDateTime examAt;

    private Boolean isFinal;

    private RoundType roundType;

    private LateSubmissionPolicy lateSubmissionPolicy;

    private LocalDateTime submissionOpen;

    @NotNull
    private LocalDateTime submissionDeadline;

    @Min(0)
    private Integer codingDurationHours;

    private LocalDateTime problemReleasedAt;

    @Min(1)
    private Integer topNAdvance;

    private Boolean wildcardEnabled;

    @Min(1)
    private Integer minTeamsFinal;

    private TiebreakRule tiebreakRule;

    /** Thời lượng thuyết trình mặc định (phút) — chỉ áp dụng khi {@code isFinal=true}. */
    @Min(1)
    @Max(60)
    private Integer defaultPresentationMinutes;

    /** Thời lượng Q&A mặc định (phút) — chỉ áp dụng khi {@code isFinal=true}. */
    @Min(1)
    @Max(60)
    private Integer defaultQaMinutes;
}
