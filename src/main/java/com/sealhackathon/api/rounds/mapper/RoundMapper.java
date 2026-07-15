package com.sealhackathon.api.rounds.mapper;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.support.RoundProblemStatementUrls;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoundMapper {

    private final RoundPresentationReadiness roundPresentationReadiness;

    public Round toEntity(CreateRoundRequest req, Hackathon hackathon) {
        boolean isFinal = Boolean.TRUE.equals(req.getIsFinal());
        RoundType roundType = req.getRoundType() != null
                ? req.getRoundType()
                : (isFinal ? RoundType.FINAL : RoundType.PRELIMINARY);
        LateSubmissionPolicy policy = req.getLateSubmissionPolicy() != null
                ? req.getLateSubmissionPolicy()
                : (isFinal ? LateSubmissionPolicy.HARD_LOCK : LateSubmissionPolicy.ALLOW_LATE_PENDING);

        return Round.builder()
                .hackathon(hackathon)
                .name(req.getName())
                .examAt(req.getExamAt())
                .isFinal(isFinal)
                .roundType(roundType)
                .lateSubmissionPolicy(policy)
                .submissionOpen(req.getSubmissionOpen())
                .submissionDeadline(req.getSubmissionDeadline())
                .codingDurationHours(req.getCodingDurationHours())
                .problemReleasedAt(req.getProblemReleasedAt())
                .topNAdvance(isFinal ? null : req.getTopNAdvance())
                .wildcardEnabled(!isFinal
                        && req.getWildcardEnabled() != null
                        && req.getWildcardEnabled())
                .minTeamsFinal(isFinal ? null : req.getMinTeamsFinal())
                .tiebreakRule(req.getTiebreakRule() == null ? TiebreakRule.PENALTY_SCORE : req.getTiebreakRule())
                .defaultPresentationMinutes(isFinal && req.getDefaultPresentationMinutes() != null
                        ? req.getDefaultPresentationMinutes()
                        : 10)
                .defaultQaMinutes(isFinal && req.getDefaultQaMinutes() != null
                        ? req.getDefaultQaMinutes()
                        : 5)
                .isActive(false)
                .scoringLocked(false)
                .forceLocked(false)
                .presentationShuffled(false)
                .build();
    }

    public void applyUpdate(Round entity, UpdateRoundRequest req) {
        entity.setName(req.getName());
        entity.setExamAt(req.getExamAt());
        entity.setSubmissionOpen(req.getSubmissionOpen());
        entity.setSubmissionDeadline(req.getSubmissionDeadline());
        if (req.getCodingDurationHours() != null) {
            entity.setCodingDurationHours(req.getCodingDurationHours());
        }
        if (req.getProblemReleasedAt() != null) {
            entity.setProblemReleasedAt(req.getProblemReleasedAt());
        }
        if (!Boolean.TRUE.equals(entity.getIsFinal())) {
            if (req.getTopNAdvance() != null) {
                entity.setTopNAdvance(req.getTopNAdvance());
            }
            if (req.getWildcardEnabled() != null) {
                entity.setWildcardEnabled(req.getWildcardEnabled());
            }
            if (req.getMinTeamsFinal() != null) {
                entity.setMinTeamsFinal(req.getMinTeamsFinal());
            }
        }
        if (req.getTiebreakRule() != null) {
            entity.setTiebreakRule(req.getTiebreakRule());
        }
        if (req.getScoringLocked() != null) {
            entity.setScoringLocked(req.getScoringLocked());
        }
        if (req.getForceLocked() != null) {
            entity.setForceLocked(req.getForceLocked());
        }
        if (req.getForceLockReason() != null) {
            entity.setForceLockReason(req.getForceLockReason());
        }
        if (Boolean.TRUE.equals(entity.getIsFinal())) {
            if (req.getDefaultPresentationMinutes() != null) {
                entity.setDefaultPresentationMinutes(req.getDefaultPresentationMinutes());
            }
            if (req.getDefaultQaMinutes() != null) {
                entity.setDefaultQaMinutes(req.getDefaultQaMinutes());
            }
        }
    }

    public RoundResponse toResponse(Round e) {
        if (e == null) {
            return null;
        }
        Integer hackathonId = e.getHackathon() == null ? null : e.getHackathon().getId();
        RoundPresentationReadiness.Flags flags = roundPresentationReadiness.evaluate(e);
        return RoundResponse.builder()
                .id(e.getId())
                .hackathonId(hackathonId)
                .trackId(null)
                .name(e.getName())
                .examAt(e.getExamAt())
                .isFinal(e.getIsFinal())
                .roundType(e.getRoundType())
                .lateSubmissionPolicy(e.getLateSubmissionPolicy())
                .submissionOpen(e.getSubmissionOpen())
                .submissionDeadline(e.getSubmissionDeadline())
                .codingDurationHours(e.getCodingDurationHours())
                .problemStatementUrl(Boolean.TRUE.equals(e.getIsFinal())
                        ? RoundProblemStatementUrls.resolveForResponse(e) : null)
                .problemStatementFilename(Boolean.TRUE.equals(e.getIsFinal())
                        ? RoundProblemStatementStorage.displayFilename(e) : null)
                .problemReleasedAt(e.getProblemReleasedAt())
                .topNAdvance(e.getTopNAdvance())
                .wildcardEnabled(e.getWildcardEnabled())
                .minTeamsFinal(e.getMinTeamsFinal())
                .tiebreakRule(e.getTiebreakRule())
                .isActive(e.getIsActive())
                .activatedAt(e.getActivatedAt())
                .scoringLocked(e.getScoringLocked())
                .scoringLockedAt(e.getScoringLockedAt())
                .scoringLockedById(e.getScoringLockedBy() == null ? null : e.getScoringLockedBy().getId())
                .forceLocked(e.getForceLocked())
                .forceLockReason(e.getForceLockReason())
                .isPublished(e.getIsPublished())
                .publishedAt(e.getPublishedAt())
                .publishedById(e.getPublishedBy() == null ? null : e.getPublishedBy().getId())
                .submissionClosedEarlyAt(e.getSubmissionClosedEarlyAt())
                .createdAt(e.getCreatedAt())
                .defaultPresentationMinutes(e.getDefaultPresentationMinutes())
                .defaultQaMinutes(e.getDefaultQaMinutes())
                .isPresentationShuffled(flags.isPresentationShuffled())
                .isPresentationsComplete(flags.isPresentationsComplete())
                .build();
    }

    public RoundSummaryResponse toSummary(Round e, int trackCount, int criteriaCount, float currentWeightTotal) {
        if (e == null) {
            return null;
        }
        RoundPresentationReadiness.Flags flags = roundPresentationReadiness.evaluate(e);
        return RoundSummaryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .examAt(e.getExamAt())
                .submissionDeadline(e.getSubmissionDeadline())
                .isActive(e.getIsActive())
                .scoringLocked(e.getScoringLocked())
                .isPublished(e.getIsPublished())
                .submissionClosedEarlyAt(e.getSubmissionClosedEarlyAt())
                .trackCount(trackCount)
                .criteriaCount(criteriaCount)
                .currentWeightTotal(currentWeightTotal)
                .isPresentationShuffled(flags.isPresentationShuffled())
                .isPresentationsComplete(flags.isPresentationsComplete())
                .build();
    }
}
