package com.se194093.be.criteria.service.impl;

import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.dto.request.BatchCreateCriteriaRequest;
import com.se194093.be.criteria.dto.request.CloneCriteriaRequest;
import com.se194093.be.criteria.dto.request.CreateCriterionRequest;
import com.se194093.be.criteria.dto.request.UpdateCriterionRequest;
import com.se194093.be.criteria.dto.response.BatchCreateResponse;
import com.se194093.be.criteria.dto.response.CloneResponse;
import com.se194093.be.criteria.dto.response.CriteriaListResponse;
import com.se194093.be.criteria.dto.response.CriterionResponse;
import com.se194093.be.criteria.service.CriteriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-04-criteria.md}.
 *
 * <p>Inject: CriteriaRepository, RoundRepository, CriteriaMapper, AuditService,
 * WeightSummaryService, ScoreRepository (check guard CRITERIA_HAS_SCORES).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CriteriaServiceImpl implements CriteriaService {

    @Override
    public CreateResult create(Integer roundId, CreateCriterionRequest req) {
        // TODO Dev:
        //  - findRound(roundId) or 404
        //  - save Criteria
        //  - audit CRITERIA_CREATE
        //  - weightWarning = weightSummaryService.warningIfNotOne(roundId)
        //  - return CreateResult(response, weightWarning)
        throw new UnsupportedOperationException("FR-04 POST /criteria - to be implemented");
    }

    @Override
    public BatchCreateResponse batchCreate(Integer roundId, BatchCreateCriteriaRequest req) {
        // TODO Dev:
        //  - findRound or 404
        //  - all-or-nothing: for each item → save
        //  - audit CRITERIA_CREATE cho từng item (hoặc 1 audit batch detail count + ids)
        //  - return BatchCreateResponse(ids, weightSummaryService.summary(roundId))
        throw new UnsupportedOperationException("FR-04 POST /criteria/batch - to be implemented");
    }

    @Override
    public CriteriaListResponse listByRound(Integer roundId) {
        // TODO Dev:
        //  - items = criteriaRepo.findByRoundIdOrderByDisplayOrderAsc(roundId).stream().map(toResponse)
        //  - summary = weightSummaryService.summary(roundId)
        //  - return CriteriaListResponse(items, summary)
        throw new UnsupportedOperationException("FR-04 GET /criteria - to be implemented");
    }

    @Override
    public CriterionResponse getById(Integer id) {
        throw new UnsupportedOperationException("FR-04 GET /criteria/{id} - to be implemented");
    }

    @Override
    public UpdateResult update(Integer id, UpdateCriterionRequest req) {
        // TODO Dev:
        //  - findById → 404
        //  - guard: scoreRepo.existsByCriterionId(id) → 409 CRITERIA_HAS_SCORES
        //  - applyUpdate; save
        //  - audit CRITERIA_UPDATE { before, after }
        //  - weightWarning = weightSummaryService.warningIfNotOne(round.id)
        throw new UnsupportedOperationException("FR-04 PUT /criteria/{id} - to be implemented");
    }

    @Override
    public Integer delete(Integer id) {
        // TODO Dev:
        //  - findById → 404
        //  - guard scoreRepo.existsByCriterionId(id) → 409 CRITERIA_HAS_SCORES
        //  - delete; audit CRITERIA_DELETE
        throw new UnsupportedOperationException("FR-04 DELETE /criteria/{id} - to be implemented");
    }

    @Override
    public CloneResponse cloneFromSource(Integer roundId, CloneCriteriaRequest req) {
        // TODO Dev:
        //  - validate sourceRoundId != roundId
        //  - findRound(target) & findRound(source) → 404
        //  - sources = criteriaRepo.findByRoundIdOrderByDisplayOrderAsc(sourceRoundId)
        //  - if sources.isEmpty() → 422 CRITERIA_CLONE_SOURCE_EMPTY
        //  - if req.replaceExisting:
        //      - if scoreRepo.existsByCriterionRoundId(roundId) → 409 CRITERIA_HAS_SCORES
        //      - criteriaRepo.deleteByRoundId(roundId)
        //  - for each src: save mapper.toClone(src, target)
        //  - audit CRITERIA_CLONE
        //  - return CloneResponse(ids, sourceRoundId, count, weightSummaryService.summary(roundId))
        throw new UnsupportedOperationException("FR-04 POST /criteria/clone - to be implemented");
    }

    @Override
    public List<Warning> wrap(Optional<Warning> single) {
        return single.map(List::of).orElse(List.of());
    }
}
