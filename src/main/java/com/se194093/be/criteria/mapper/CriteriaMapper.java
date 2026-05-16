package com.se194093.be.criteria.mapper;

import com.se194093.be.criteria.dto.request.CreateCriterionRequest;
import com.se194093.be.criteria.dto.request.UpdateCriterionRequest;
import com.se194093.be.criteria.dto.response.CriterionResponse;
import com.se194093.be.criteria.entity.Criteria;
import com.se194093.be.rounds.entity.Round;
import org.springframework.stereotype.Component;

@Component
public class CriteriaMapper {

    public Criteria toEntity(CreateCriterionRequest req, Round round) {
        return Criteria.builder()
                .round(round)
                .name(req.getName())
                .type(req.getType())
                .weight(req.getWeight())
                .maxScore(req.getMaxScore())
                .description(req.getDescription())
                .rubricUrl(req.getRubricUrl())
                .displayOrder(req.getDisplayOrder() == null ? 0 : req.getDisplayOrder())
                .build();
    }

    public Criteria toClone(Criteria source, Round targetRound) {
        return Criteria.builder()
                .round(targetRound)
                .sourceCriteria(source)
                .name(source.getName())
                .type(source.getType())
                .weight(source.getWeight())
                .maxScore(source.getMaxScore())
                .description(source.getDescription())
                .rubricUrl(source.getRubricUrl())
                .displayOrder(source.getDisplayOrder())
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
