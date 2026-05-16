package com.se194093.be.rounds.mapper;

import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.dto.request.UpdateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.dto.response.RoundSummaryResponse;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.value_object.TiebreakRule;
import com.se194093.be.tracks.entity.Track;
import org.springframework.stereotype.Component;

@Component
public class RoundMapper {

    public Round toEntity(CreateRoundRequest req, Track track) {
        return Round.builder()
                .track(track)
                .name(req.getName())
                .sequenceOrder(req.getSequenceOrder())
                .submissionOpen(req.getSubmissionOpen())
                .submissionDeadline(req.getSubmissionDeadline())
                .codingDurationHours(req.getCodingDurationHours())
                .problemStatementUrl(req.getProblemStatementUrl())
                .problemReleasedAt(req.getProblemReleasedAt())
                .topNAdvance(req.getTopNAdvance())
                .wildcardEnabled(req.getWildcardEnabled() != null && req.getWildcardEnabled())
                .minTeamsFinal(req.getMinTeamsFinal())
                .tiebreakRule(req.getTiebreakRule() == null ? TiebreakRule.PENALTY_SCORE : req.getTiebreakRule())
                .isActive(false)
                .scoringLocked(false)
                .forceLocked(false)
                .build();
    }

    public void applyUpdate(Round entity, UpdateRoundRequest req) {
        entity.setName(req.getName());
        entity.setSequenceOrder(req.getSequenceOrder());
        entity.setSubmissionOpen(req.getSubmissionOpen());
        entity.setSubmissionDeadline(req.getSubmissionDeadline());
        entity.setCodingDurationHours(req.getCodingDurationHours());
        entity.setProblemStatementUrl(req.getProblemStatementUrl());
        entity.setProblemReleasedAt(req.getProblemReleasedAt());
        entity.setTopNAdvance(req.getTopNAdvance());
        if (req.getWildcardEnabled() != null) {
            entity.setWildcardEnabled(req.getWildcardEnabled());
        }
        entity.setMinTeamsFinal(req.getMinTeamsFinal());
        if (req.getTiebreakRule() != null) {
            entity.setTiebreakRule(req.getTiebreakRule());
        }
        if (req.getScoringLocked() != null) {
            entity.setScoringLocked(req.getScoringLocked());
        }
        if (req.getForceLocked() != null) {
            entity.setForceLocked(req.getForceLocked());
        }
        entity.setForceLockReason(req.getForceLockReason());
    }

    public RoundResponse toResponse(Round e) {
        if (e == null) {
            return null;
        }
        return RoundResponse.builder()
                .id(e.getId())
                .trackId(e.getTrack() == null ? null : e.getTrack().getId())
                .name(e.getName())
                .sequenceOrder(e.getSequenceOrder())
                .submissionOpen(e.getSubmissionOpen())
                .submissionDeadline(e.getSubmissionDeadline())
                .codingDurationHours(e.getCodingDurationHours())
                .problemStatementUrl(e.getProblemStatementUrl())
                .problemReleasedAt(e.getProblemReleasedAt())
                .topNAdvance(e.getTopNAdvance())
                .wildcardEnabled(e.getWildcardEnabled())
                .minTeamsFinal(e.getMinTeamsFinal())
                .tiebreakRule(e.getTiebreakRule())
                .isActive(e.getIsActive())
                .scoringLocked(e.getScoringLocked())
                .scoringLockedAt(e.getScoringLockedAt())
                .scoringLockedById(e.getScoringLockedBy() == null ? null : e.getScoringLockedBy().getId())
                .forceLocked(e.getForceLocked())
                .forceLockReason(e.getForceLockReason())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public RoundSummaryResponse toSummary(Round e, int criteriaCount, float currentWeightTotal) {
        if (e == null) {
            return null;
        }
        return RoundSummaryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .sequenceOrder(e.getSequenceOrder())
                .submissionDeadline(e.getSubmissionDeadline())
                .isActive(e.getIsActive())
                .scoringLocked(e.getScoringLocked())
                .criteriaCount(criteriaCount)
                .currentWeightTotal(currentWeightTotal)
                .build();
    }
}
