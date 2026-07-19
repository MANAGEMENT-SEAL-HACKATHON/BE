package com.sealhackathon.api.rbl.calibration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class CalibrationDtos {
    private CalibrationDtos() {}

    public record CreatePromptRequest(
            @NotNull Integer hackathonId,
            @NotNull Integer roundId,
            @NotBlank @Size(max = 255) String title,
            String description,
            Integer sampleSubmissionId) {}

    public record SubmitScoresRequest(@NotEmpty List<@Valid ScoreInput> scores) {}

    public record ScoreInput(
            @NotNull Integer criterionId,
            @NotNull @DecimalMin("0.0") Float scoreValue,
            String comment) {}

    public record CriterionView(
            Integer id, String name, String description, Integer maxScore, Float weight) {}

    public record PromptView(
            Integer id,
            Integer hackathonId,
            Integer roundId,
            String title,
            String description,
            Integer sampleSubmissionId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime closedAt,
            List<CriterionView> criteria) {}

    public record AnonymousJudgeScores(String label, List<CriterionScoreView> scores) {}

    public record CriterionScoreView(Integer criterionId, String criterionName, Float scoreValue) {}

    public record DistributionView(
            Integer promptId, String promptTitle, List<AnonymousJudgeScores> judges) {}
}
