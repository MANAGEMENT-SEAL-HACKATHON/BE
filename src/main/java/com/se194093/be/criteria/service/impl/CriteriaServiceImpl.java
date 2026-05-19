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
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CriteriaServiceImpl implements CriteriaService {

    private final CriteriaRepository criteriaRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaMapper criteriaMapper;
    private final AuditService auditService;
    private final WeightSummaryService weightSummaryService;
    private final ScorePlaceholderRepository scoreRepository;

    @Override
    public CreateResult createForTrack(Integer trackId, CreateCriterionRequest req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        Criteria saved = criteriaRepository.save(criteriaMapper.toEntityForTrack(req, track));
        CriterionResponse response = criteriaMapper.toResponse(saved);
        auditService.log(AuditAction.CRITERIA_CREATE, "criteria", saved.getId(),
                Map.of("trackId", trackId, "snapshot", response));
        return new CreateResult(response, weightSummaryService.warningIfNotOneForTrack(trackId));
    }

    @Override
    public CreateResult createForFinalRound(Integer finalRoundId, CreateCriterionRequest req) {
        Round round = loadFinalRound(finalRoundId);
        Criteria saved = criteriaRepository.save(criteriaMapper.toEntityForFinalRound(req, round));
        CriterionResponse response = criteriaMapper.toResponse(saved);
        auditService.log(AuditAction.CRITERIA_CREATE, "criteria", saved.getId(),
                Map.of("roundId", finalRoundId, "snapshot", response));
        return new CreateResult(response, weightSummaryService.warningIfNotOneForFinalRound(finalRoundId));
    }

    @Override
    @Deprecated
    public CreateResult create(Integer roundId, CreateCriterionRequest req) {
        return createForFinalRound(roundId, req);
    }

    @Override
    public BatchCreateResponse batchCreateForTrack(Integer trackId, BatchCreateCriteriaRequest req) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        List<Integer> createdIds = new ArrayList<>();
        for (CreateCriterionRequest item : req.getItems()) {
            Criteria saved = criteriaRepository.save(criteriaMapper.toEntityForTrack(item, track));
            createdIds.add(saved.getId());
        }
        auditService.log(AuditAction.CRITERIA_CREATE, "criteria", null,
                Map.of("trackId", trackId, "batch", true, "count", createdIds.size()));
        return BatchCreateResponse.builder()
                .createdIds(createdIds)
                .weightSummary(weightSummaryService.summaryForTrack(trackId))
                .build();
    }

    @Override
    public BatchCreateResponse batchCreateForFinalRound(Integer finalRoundId, BatchCreateCriteriaRequest req) {
        Round round = loadFinalRound(finalRoundId);
        List<Integer> createdIds = new ArrayList<>();
        for (CreateCriterionRequest item : req.getItems()) {
            Criteria saved = criteriaRepository.save(criteriaMapper.toEntityForFinalRound(item, round));
            createdIds.add(saved.getId());
        }
        auditService.log(AuditAction.CRITERIA_CREATE, "criteria", null,
                Map.of("roundId", finalRoundId, "batch", true, "count", createdIds.size()));
        return BatchCreateResponse.builder()
                .createdIds(createdIds)
                .weightSummary(weightSummaryService.summaryForFinalRound(finalRoundId))
                .build();
    }

    @Override
    @Deprecated
    public BatchCreateResponse batchCreate(Integer roundId, BatchCreateCriteriaRequest req) {
        return batchCreateForFinalRound(roundId, req);
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaListResponse listByTrack(Integer trackId) {
        List<CriterionResponse> items = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId)
                .stream().map(criteriaMapper::toResponse).toList();
        return CriteriaListResponse.builder()
                .items(items)
                .weightSummary(weightSummaryService.summaryForTrack(trackId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaListResponse listByFinalRound(Integer finalRoundId) {
        List<CriterionResponse> items = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRoundId)
                .stream().map(criteriaMapper::toResponse).toList();
        return CriteriaListResponse.builder()
                .items(items)
                .weightSummary(weightSummaryService.summaryForFinalRound(finalRoundId))
                .build();
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public CriteriaListResponse listByRound(Integer roundId) {
        return listByFinalRound(roundId);
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
        CriterionResponse before = criteriaMapper.toResponse(c);
        criteriaMapper.applyUpdate(c, req);
        Criteria saved = criteriaRepository.save(c);
        CriterionResponse after = criteriaMapper.toResponse(saved);
        auditService.logBeforeAfter(AuditAction.CRITERIA_UPDATE, "criteria", saved.getId(), before, after);
        return new UpdateResult(after, weightWarningForCriteria(saved));
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
    public CloneResponse cloneFromSourceForTrack(Integer trackId, CloneCriteriaRequest req) {
        Track target = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        if (req.getSourceTrackId() == null) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "sourceTrackId bắt buộc khi clone vào Track",
                    Map.of("trackId", trackId));
        }
        List<Criteria> sources = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(req.getSourceTrackId());
        if (sources.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "Track nguồn không có Criteria", Map.of("sourceTrackId", req.getSourceTrackId()));
        }
        List<Integer> createdIds = saveClonesForTrack(sources, target);
        return CloneResponse.builder()
                .createdIds(createdIds)
                .count(createdIds.size())
                .weightSummary(weightSummaryService.summaryForTrack(trackId))
                .build();
    }

    @Override
    public CloneResponse cloneFromSourceForFinalRound(Integer finalRoundId, CloneCriteriaRequest req) {
        Round target = loadFinalRound(finalRoundId);
        List<Criteria> sources = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(req.getSourceRoundId());
        if (sources.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "Round nguồn không có Criteria", Map.of("sourceRoundId", req.getSourceRoundId()));
        }
        List<Integer> createdIds = new ArrayList<>();
        for (Criteria src : sources) {
            Criteria saved = criteriaRepository.save(criteriaMapper.toCloneForFinalRound(src, target));
            createdIds.add(saved.getId());
        }
        return CloneResponse.builder()
                .createdIds(createdIds)
                .sourceRoundId(req.getSourceRoundId())
                .count(createdIds.size())
                .weightSummary(weightSummaryService.summaryForFinalRound(finalRoundId))
                .build();
    }

    @Override
    @Deprecated
    public CloneResponse cloneFromSource(Integer roundId, CloneCriteriaRequest req) {
        return cloneFromSourceForFinalRound(roundId, req);
    }

    @Override
    public List<Warning> wrap(Optional<Warning> single) {
        return single.map(List::of).orElse(List.of());
    }

    private Round loadFinalRound(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_FINAL_FOR_CRITERIA,
                    "Criteria qua round_id chỉ cho Round Chung kết (is_final=TRUE)",
                    Map.of("roundId", roundId));
        }
        return round;
    }

    private List<Integer> saveClonesForTrack(List<Criteria> sources, Track target) {
        List<Integer> createdIds = new ArrayList<>();
        for (Criteria src : sources) {
            Criteria saved = criteriaRepository.save(criteriaMapper.toCloneForTrack(src, target));
            createdIds.add(saved.getId());
        }
        return createdIds;
    }

    private Optional<Warning> weightWarningForCriteria(Criteria c) {
        if (c.getTrack() != null) {
            return weightSummaryService.warningIfNotOneForTrack(c.getTrack().getId());
        }
        if (c.getRound() != null) {
            return weightSummaryService.warningIfNotOneForFinalRound(c.getRound().getId());
        }
        return Optional.empty();
    }
}
