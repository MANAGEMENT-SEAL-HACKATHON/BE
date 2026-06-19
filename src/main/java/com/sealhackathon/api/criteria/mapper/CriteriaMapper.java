package com.sealhackathon.api.criteria.mapper;

import com.sealhackathon.api.criteria.dto.request.CreateCriterionRequest;
import com.sealhackathon.api.criteria.dto.request.UpdateCriterionRequest;
import com.sealhackathon.api.criteria.dto.response.CriterionResponse;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.springframework.stereotype.Component;

@Component
public class CriteriaMapper {

    public Criteria toEntityForTrack(CreateCriterionRequest req, Track track) {
        return Criteria.builder()
                .track(track)
                .round(null)
                .name(req.getName())
                .type(req.getType())
                .weight(req.getWeight())
                .maxScore(req.getMaxScore())
                .description(req.getDescription())
                .rubricUrl(req.getRubricUrl())
                .displayOrder(req.getDisplayOrder() == null ? 1 : req.getDisplayOrder())
                .build();
    }

    public Criteria toEntityForFinalRound(CreateCriterionRequest req, Round finalRound) {
        return Criteria.builder()
                .track(null)
                .round(finalRound)
                .name(req.getName())
                .type(req.getType())
                .weight(req.getWeight())
                .maxScore(req.getMaxScore())
                .description(req.getDescription())
                .rubricUrl(req.getRubricUrl())
                .displayOrder(req.getDisplayOrder() == null ? 1 : req.getDisplayOrder())
                .build();
    }

    /** @deprecated */
    @Deprecated
    public Criteria toEntity(CreateCriterionRequest req, Round round) {
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return toEntityForFinalRound(req, round);
        }
        throw new IllegalArgumentException("Round Sơ loại: dùng toEntityForTrack");
    }

    /**
     * Sao chép nội dung sang track đích — không gắn {@code source_criteria_id} để từng dòng xóa/sửa độc lập.
     */
    public Criteria toCloneForTrack(Criteria source, Track targetTrack, int displayOrder) {
        return Criteria.builder()
                .track(targetTrack)
                .round(null)
                .sourceCriteria(null)
                .name(source.getName())
                .type(source.getType())
                .weight(source.getWeight())
                .maxScore(source.getMaxScore())
                .description(source.getDescription())
                .rubricUrl(source.getRubricUrl())
                .displayOrder(displayOrder)
                .build();
    }

    public Criteria toCloneForFinalRound(Criteria source, Round targetRound, int displayOrder) {
        return Criteria.builder()
                .track(null)
                .round(targetRound)
                .sourceCriteria(null)
                .name(source.getName())
                .type(source.getType())
                .weight(source.getWeight())
                .maxScore(source.getMaxScore())
                .description(source.getDescription())
                .rubricUrl(source.getRubricUrl())
                .displayOrder(displayOrder)
                .build();
    }

    public void applyUpdate(Criteria entity, UpdateCriterionRequest req) {
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setWeight(req.getWeight());
        entity.setMaxScore(req.getMaxScore());
        entity.setDescription(req.getDescription());
        entity.setRubricUrl(req.getRubricUrl());
        if (req.getDisplayOrder() != null) {
            entity.setDisplayOrder(req.getDisplayOrder());
        }
    }

    public CriterionResponse toResponse(Criteria e) {
        if (e == null) {
            return null;
        }
        return CriterionResponse.builder()
                .id(e.getId())
                .trackId(e.getTrack() == null ? null : e.getTrack().getId())
                .roundId(e.getRound() == null ? null : e.getRound().getId())
                .sourceCriteriaId(e.getSourceCriteria() == null ? null : e.getSourceCriteria().getId())
                .name(e.getName())
                .type(e.getType())
                .weight(e.getWeight())
                .maxScore(e.getMaxScore())
                .description(e.getDescription())
                .rubricUrl(e.getRubricUrl())
                .displayOrder(e.getDisplayOrder())
                .build();
    }
}
