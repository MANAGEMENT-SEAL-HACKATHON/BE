package com.sealhackathon.api.criteria.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.dto.request.CriteriaTemplateRequest;
import com.sealhackathon.api.criteria.dto.response.CriteriaTemplateResponse;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.entity.CriteriaTemplate;
import com.sealhackathon.api.criteria.entity.CriteriaTemplateItem;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.repository.CriteriaTemplateRepository;
import com.sealhackathon.api.criteria.service.CriteriaTemplateService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CriteriaTemplateServiceImpl implements CriteriaTemplateService {
    private final CriteriaTemplateRepository templateRepository;
    private final CriteriaRepository criteriaRepository;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final ScoreRepository scoreRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaTemplateResponse> list() {
        return templateRepository.findAllByOrderByIsDefaultDescNameAsc().stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaTemplateResponse get(Integer id) {
        return response(load(id));
    }

    @Override
    public CriteriaTemplateResponse create(CriteriaTemplateRequest request) {
        CriteriaTemplate template = CriteriaTemplate.builder()
                .name(request.name().trim())
                .description(request.description())
                .isDefault(Boolean.TRUE.equals(request.isDefault()))
                .createdBy(userRepository.findById(currentUserAccessor.currentUserId()).orElse(null))
                .build();
        replaceItems(template, request.items());
        return response(templateRepository.save(template));
    }

    @Override
    public CriteriaTemplateResponse update(Integer id, CriteriaTemplateRequest request) {
        CriteriaTemplate template = load(id);
        template.setName(request.name().trim());
        template.setDescription(request.description());
        template.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        replaceItems(template, request.items());
        return response(templateRepository.save(template));
    }

    @Override
    public void delete(Integer id) {
        templateRepository.delete(load(id));
    }

    @Override
    public ApplyResult applyToTrack(Integer templateId, Integer trackId, boolean replaceExisting) {
        CriteriaTemplate template = load(templateId);
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        List<Criteria> existing = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId);
        prepareTarget(existing, replaceExisting, Map.of("trackId", trackId));
        List<Integer> ids = new ArrayList<>();
        for (CriteriaTemplateItem item : template.getItems()) {
            Criteria saved = criteriaRepository.save(toCriterion(item, track, null));
            ids.add(saved.getId());
        }
        return new ApplyResult(ids, ids.size());
    }

    @Override
    public ApplyResult applyToFinalRound(Integer templateId, Integer roundId, boolean replaceExisting) {
        CriteriaTemplate template = load(templateId);
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_FINAL_FOR_CRITERIA,
                    "Chỉ được áp dụng mẫu vào vòng Chung kết");
        }
        List<Criteria> existing = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId);
        prepareTarget(existing, replaceExisting, Map.of("roundId", roundId));
        List<Integer> ids = new ArrayList<>();
        for (CriteriaTemplateItem item : template.getItems()) {
            Criteria saved = criteriaRepository.save(toCriterion(item, null, round));
            ids.add(saved.getId());
        }
        return new ApplyResult(ids, ids.size());
    }

    private void prepareTarget(List<Criteria> existing, boolean replaceExisting, Map<String, Object> details) {
        if (!existing.isEmpty() && !replaceExisting) {
            throw new BusinessRuleException(ErrorCode.CRITERIA_TARGET_HAS_EXISTING,
                    "Đích đã có tiêu chí; bật replaceExisting để thay thế", details);
        }
        if (!replaceExisting) return;
        for (Criteria criterion : existing) {
            if (scoreRepository.countByCriteriaId(criterion.getId()) > 0) {
                throw new ConflictException(ErrorCode.CRITERIA_HAS_SCORES,
                        "Tiêu chí đã có điểm nên không thể thay thế", details);
            }
        }
        criteriaRepository.deleteAll(existing);
        criteriaRepository.flush();
    }

    private CriteriaTemplate load(Integer id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CriteriaTemplate", id));
    }

    private static void replaceItems(CriteriaTemplate template, List<CriteriaTemplateRequest.Item> requests) {
        template.getItems().clear();
        for (CriteriaTemplateRequest.Item request : requests) {
            template.getItems().add(CriteriaTemplateItem.builder()
                    .template(template).name(request.name().trim()).type(request.type())
                    .weight(request.weight()).maxScore(request.maxScore())
                    .description(request.description()).displayOrder(request.displayOrder())
                    .isTiebreakerPriority(Boolean.TRUE.equals(request.isTiebreakerPriority()))
                    .build());
        }
    }

    private static Criteria toCriterion(CriteriaTemplateItem item, Track track, Round round) {
        return Criteria.builder()
                .track(track).round(round).name(item.getName()).type(item.getType())
                .weight(item.getWeight()).maxScore(item.getMaxScore())
                .description(item.getDescription()).displayOrder(item.getDisplayOrder())
                .isTiebreakerPriority(Boolean.TRUE.equals(item.getIsTiebreakerPriority()))
                .build();
    }

    private CriteriaTemplateResponse response(CriteriaTemplate template) {
        return new CriteriaTemplateResponse(
                template.getId(), template.getName(), template.getDescription(), template.getIsDefault(),
                template.getCreatedBy() == null ? null : template.getCreatedBy().getId(),
                template.getCreatedAt(), template.getUpdatedAt(),
                template.getItems().stream().map(item -> new CriteriaTemplateResponse.Item(
                        item.getId(), item.getName(), item.getType(), item.getWeight(), item.getMaxScore(),
                        item.getDescription(), item.getDisplayOrder(),
                        Boolean.TRUE.equals(item.getIsTiebreakerPriority()))).toList());
    }
}
