package com.sealhackathon.api.criteria.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.criteria.dto.request.BatchCreateCriteriaRequest;
import com.sealhackathon.api.criteria.dto.request.CloneCriteriaRequest;
import com.sealhackathon.api.criteria.dto.request.CreateCriterionRequest;
import com.sealhackathon.api.criteria.dto.request.UpdateCriterionRequest;
import com.sealhackathon.api.criteria.dto.response.BatchCreateResponse;
import com.sealhackathon.api.criteria.dto.response.CloneResponse;
import com.sealhackathon.api.criteria.dto.response.CriteriaListResponse;
import com.sealhackathon.api.criteria.dto.response.CriterionResponse;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.mapper.CriteriaMapper;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.CriteriaService;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScorePlaceholderRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
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
        int deletedCount = replaceExistingForTrackIfRequested(trackId, req);
        List<Integer> createdIds = saveClonesForTrack(sources, target);
        auditService.log(AuditAction.CRITERIA_CLONE, "criteria", trackId, Map.of(
                "targetTrackId", trackId,
                "sourceTrackId", req.getSourceTrackId(),
                "replaceExisting", Boolean.TRUE.equals(req.getReplaceExisting()),
                "deletedCount", deletedCount,
                "clonedCount", createdIds.size()));
        return CloneResponse.builder()
                .createdIds(createdIds)
                .count(createdIds.size())
                .weightSummary(weightSummaryService.summaryForTrack(trackId))
                .build();
    }

    @Override
    public CloneResponse cloneFromSourceForFinalRound(Integer finalRoundId, CloneCriteriaRequest req) {
        Round target = loadFinalRound(finalRoundId);
        if (req.getSourceRoundId() == null) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "sourceRoundId bắt buộc khi clone vào Round Chung kết",
                    Map.of("roundId", finalRoundId));
        }
        List<Criteria> sources = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(req.getSourceRoundId());
        if (sources.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_CLONE_SOURCE_EMPTY,
                    "Round nguồn không có Criteria", Map.of("sourceRoundId", req.getSourceRoundId()));
        }
        int deletedCount = replaceExistingForFinalRoundIfRequested(finalRoundId, req);
        List<Integer> createdIds = new ArrayList<>();
        for (Criteria src : sources) {
            Criteria saved = criteriaRepository.save(criteriaMapper.toCloneForFinalRound(src, target));
            createdIds.add(saved.getId());
        }
        auditService.log(AuditAction.CRITERIA_CLONE, "criteria", finalRoundId, Map.of(
                "targetRoundId", finalRoundId,
                "sourceRoundId", req.getSourceRoundId(),
                "replaceExisting", Boolean.TRUE.equals(req.getReplaceExisting()),
                "deletedCount", deletedCount,
                "clonedCount", createdIds.size()));
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

    /**
     * FR-04 clone: {@code replaceExisting=true} → xóa hết criteria của track đích trước khi clone (block nếu có scores).
     */
    private int replaceExistingForTrackIfRequested(Integer trackId, CloneCriteriaRequest req) {
        boolean replace = Boolean.TRUE.equals(req.getReplaceExisting());
        if (!replace) {
            return 0;
        }
        List<Criteria> existing = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
        for (Criteria c : existing) {
            if (scoreRepository.countByCriteriaId(c.getId()) > 0) {
                throw new ConflictException(ErrorCode.CRITERIA_HAS_SCORES,
                        "Track đích đã có scores — không thể replaceExisting",
                        Map.of("trackId", trackId, "criterionId", c.getId()));
            }
        }
        for (Criteria c : existing) {
            criteriaRepository.delete(c);
        }
        return existing.size();
    }

    private int replaceExistingForFinalRoundIfRequested(Integer finalRoundId, CloneCriteriaRequest req) {
        if (!Boolean.TRUE.equals(req.getReplaceExisting())) {
            return 0;
        }
        if (scoreRepository.countByRoundId(finalRoundId) > 0) {
            throw new ConflictException(ErrorCode.CRITERIA_HAS_SCORES,
                    "Round Chung kết đích đã có scores — không thể replaceExisting",
                    Map.of("roundId", finalRoundId));
        }
        List<Criteria> existing = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRoundId);
        for (Criteria c : existing) {
            criteriaRepository.delete(c);
        }
        return existing.size();
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
