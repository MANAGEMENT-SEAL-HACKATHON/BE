package com.sealhackathon.api.appeals.mapper;

import com.sealhackathon.api.appeals.dto.response.AppealEvidenceResponse;
import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.entity.AppealEvidence;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppealMapper {

    public AppealResponse toResponse(Appeal appeal) {
        if (appeal == null) {
            return null;
        }
        List<AppealEvidenceResponse> evidences = appeal.getEvidences() == null
                ? List.of()
                : appeal.getEvidences().stream().map(this::toEvidence).toList();

        return AppealResponse.builder()
                .id(appeal.getId())
                .teamId(appeal.getTeam() != null ? appeal.getTeam().getId() : null)
                .teamName(appeal.getTeam() != null ? appeal.getTeam().getTeamName() : null)
                .roundId(appeal.getRound() != null ? appeal.getRound().getId() : null)
                .roundName(appeal.getRound() != null ? appeal.getRound().getName() : null)
                .reason(appeal.getReason())
                .evidenceUrl(appeal.getEvidenceUrl())
                .evidences(evidences)
                .status(appeal.getStatus())
                .decisionNote(appeal.getDecisionNote())
                .reviewedById(appeal.getReviewedBy() != null ? appeal.getReviewedBy().getId() : null)
                .reviewedAt(appeal.getReviewedAt())
                .createdAt(appeal.getCreatedAt())
                .updatedAt(appeal.getUpdatedAt())
                .version(appeal.getVersion())
                .build();
    }

    public AppealEvidenceResponse toEvidence(AppealEvidence evidence) {
        return AppealEvidenceResponse.builder()
                .id(evidence.getId())
                .url(evidence.getUrl())
                .type(evidence.getType())
                .caption(evidence.getCaption())
                .displayOrder(evidence.getDisplayOrder())
                .build();
    }
}
