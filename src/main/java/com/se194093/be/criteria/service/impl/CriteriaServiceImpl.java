package com.se194093.be.criteria.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.dto.request.BatchCreateCriteriaRequest;
import com.se194093.be.criteria.dto.request.CloneCriteriaRequest;
import com.se194093.be.criteria.dto.request.CreateCriterionRequest;
import com.se194093.be.criteria.dto.request.UpdateCriterionRequest;
import com.se194093.be.criteria.dto.response.BatchCreateResponse;
import com.se194093.be.criteria.dto.response.CloneResponse;
import com.se194093.be.criteria.dto.response.CriteriaListResponse;
import com.se194093.be.criteria.dto.response.CriterionResponse;
import com.se194093.be.criteria.entity.Criteria;
import com.se194093.be.criteria.mapper.CriteriaMapper;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.CriteriaService;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.scores.repository.ScorePlaceholderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FR-04 Criteria CRUD + batch + clone. Validate weight mức WARN MỀM (qua
 * {@link WeightSummaryService#warningIfNotOne}). Guard CRITERIA_HAS_SCORES (stub) cho
 * update/delete/clone replace.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CriteriaServiceImpl implements CriteriaService {

    private final CriteriaRepository criteriaRepository;
    private final RoundRepository roundRepository;
    private final CriteriaMapper criteriaMapper;
    private final AuditService auditService;
    private final WeightSummaryService weightSummaryService;
    private final ScorePlaceholderRepository scoreRepository;

    @Override
    public CreateResult create(Integer roundId, CreateCriterionRequest req) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        Criteria entity = criteriaMapper.toEntity(req, round);
        Criteria saved = criteriaRepository.save(entity);

        CriterionResponse response = criteriaMapper.toResponse(saved);
        auditService.log(AuditAction.CRITERIA_CREATE, "criteria", saved.getId(),
                Map.of("roundId", roundId, "snapshot", response));
        return new CreateResult(response, weightSummaryService.warningIfNotOne(roundId));
    }

    @Override
    public BatchCreateResponse batchCreate(Integer roundId, BatchCreateCriteriaRequest req) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        List<Integer> createdIds = new ArrayList<>();
        for (CreateCriterionRequest item : req.getItems()) {
            Criteria entity = criteriaMapper.toEntity(item, round);
            Criteria saved = criteriaRepository.save(entity);
            createdIds.add(saved.getId());
        }
        auditService.log(AuditAction.CRITERIA_CREATE, "criteria", null,
                Map.of("roundId", roundId, "batch", true, "count", createdIds.size(),
                       "createdIds", createdIds));
        return BatchCreateResponse.builder()
                .createdIds(createdIds)
                .weightSummary(weightSummaryService.summary(roundId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaListResponse listByRound(Integer roundId) {
        List<CriterionResponse> items = criteriaRepository.findByRoundIdOrderByDisplayOrderAsc(roundId)
                .stream().map(criteriaMapper::toResponse).toList();
        return CriteriaListResponse.builder()
                .items(items)
                .weightSummary(weightSummaryService.summary(roundId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CriterionResponse getById(Integer id) {
        Criteria c = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion", id));
        return criteriaMapper.toResponse(c);
    }

    @Override
    public UpdateResult update(Integer id, UpdateCriterionRequest req) {
        Criteria c = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion", id));
        if (scoreRepository.countByCriteriaId(id) > 0) {
            throw new ConflictException(ErrorCode.CRITERIA_HAS_SCORES,
                    "Criterion đã có scores — không thể sửa");
        }
        Integer roundId = c.getRound() == null ? null : c.getRound().getId();

        CriterionResponse before = criteriaMapper.toResponse(c);
        criteriaMapper.applyUpdate(c, req);
        Criteria saved = criteriaRepository.save(c);
        CriterionResponse after = criteriaMapper.toResponse(saved);

        auditService.logBeforeAfter(AuditAction.CRITERIA_UPDATE, "criteria", saved.getId(),
                before, after);
        Optional<Warning> warn = (roundId == null) ? Optional.empty()
                : weightSummaryService.warningIfNotOne(roundId);
        return new UpdateResult(after, warn);
    }

    @Override
    public Integer delete(Integer id) {
        Criteria c = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion", id));
        if (scoreRepository.countByCriteriaId(id) > 0) {
            throw new ConflictException(ErrorCode.CRITERIA_HAS_SCORES,
                    "Criterion đã có scores — không thể xóa");
        }
        CriterionResponse snapshot = criteriaMapper.toResponse(c);
        criteriaRepository.delete(c);
        auditService.log(AuditAction.CRITERIA_DELETE, "criteria", id, Map.of("snapshot", snapshot));
        return id;
    }

    @Override
    public CloneResponse cloneFromSource(Integer roundId, CloneCriteriaRequest req) {
        if (roundId.equals(req.getSourceRoundId())) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "sourceRoundId phải khác roundId đích",
                    Map.of("roundId", roundId));
        }
        Round target = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        roundRepository.findById(req.getSourceRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round (source)", req.getSourceRoundId()));

        List<Criteria> sources = criteriaRepository
                .findByRoundIdOrderByDisplayOrderAsc(req.getSourceRoundId());
        if (sources.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "Round nguồn không có Criteria để clone",
                    Map.of("sourceRoundId", req.getSourceRoundId()));
        }

        boolean replace = Boolean.TRUE.equals(req.getReplaceExisting());
        if (replace) {
            if (scoreRepository.countByRoundId(roundId) > 0) {
                throw new ConflictException(ErrorCode.CRITERIA_HAS_SCORES,
                        "Round đích đã có scores — không thể replace");
            }
            criteriaRepository.deleteByRoundId(roundId);
        }

        List<Integer> createdIds = new ArrayList<>();
        for (Criteria src : sources) {
            Criteria cloned = criteriaMapper.toClone(src, target);
            Criteria saved = criteriaRepository.save(cloned);
            createdIds.add(saved.getId());
        }
        auditService.log(AuditAction.CRITERIA_CLONE, "criteria", null, Map.of(
                "roundId",        roundId,
                "sourceRoundId",  req.getSourceRoundId(),
                "count",          createdIds.size(),
                "replaceExisting", replace
        ));
        return CloneResponse.builder()
                .createdIds(createdIds)
                .sourceRoundId(req.getSourceRoundId())
                .count(createdIds.size())
                .weightSummary(weightSummaryService.summary(roundId))
                .build();
    }

    @Override
    public List<Warning> wrap(Optional<Warning> single) {
        return single.map(List::of).orElse(List.of());
    }
}
