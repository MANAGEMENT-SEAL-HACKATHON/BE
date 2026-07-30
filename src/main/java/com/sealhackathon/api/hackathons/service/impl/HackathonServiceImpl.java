package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.dto.request.CreateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.request.UpdateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonSummaryResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.mapper.HackathonMapper;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonService;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.support.HackathonBannerStorageService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implementation cho {@link HackathonService} — FR-01 CRUD.
 *
 * <p>Audit cho mọi mutation. Validate UNIQUE (name+season+year, slug). State guard DRAFT cho
 * update/delete. Guard child entity (Track/Event) khi delete.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HackathonServiceImpl implements HackathonService {

    private final HackathonRepository hackathonRepository;
    private final HackathonMapper hackathonMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final EventRepository eventRepository;
    private final HackathonArchiveGuard archiveGuard;
    private final HackathonBannerStorageService bannerStorageService;
    private final HackathonCloneSupport hackathonCloneSupport;

    @Override
    public HackathonResponse create(CreateHackathonRequest req) {
        if (hackathonRepository.existsByNameAndSeasonAndYear(req.getName(), req.getSeason(), req.getYear())) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE,
                    "Hackathon đã tồn tại cho (name=%s, season=%s, year=%d)"
                            .formatted(req.getName(), req.getSeason(), req.getYear()));
        }
        if (hackathonRepository.existsBySlug(req.getSlug())) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE,
                    "Slug đã được sử dụng: " + req.getSlug());
        }
        validateEventStartAfterRegistrationEnd(req.getRegistrationEnd(), req.getEventStart());
        if (req.getAppealWindowMinutes() != null) {
            validateAppealWindowMinutes(req.getAppealWindowMinutes());
        }

        Hackathon entity = hackathonMapper.toEntity(req);
        entity.setStatus(HackathonStatus.DRAFT);
        Integer uid = currentUserAccessor.currentUserId();
        if (uid != null) {
            entity.setCreatedBy(User.builder().id(uid).build());
        }
        Hackathon saved = hackathonRepository.save(entity);

        HackathonResponse response = hackathonMapper.toResponse(saved);
        auditService.log(AuditAction.HACKATHON_CREATE, "hackathons", saved.getId(),
                Map.of("snapshot", response));
        return response;
    }

    @Override
    public HackathonResponse cloneFrom(Integer sourceId, CreateHackathonRequest req) {
        Hackathon source = hackathonRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", sourceId));

        if (hackathonRepository.existsByNameAndSeasonAndYear(req.getName(), req.getSeason(), req.getYear())) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE,
                    "Hackathon đã tồn tại cho (name=%s, season=%s, year=%d)"
                            .formatted(req.getName(), req.getSeason(), req.getYear()));
        }
        if (hackathonRepository.existsBySlug(req.getSlug())) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE,
                    "Slug đã được sử dụng: " + req.getSlug());
        }
        validateEventStartAfterRegistrationEnd(req.getRegistrationEnd(), req.getEventStart());
        if (req.getAppealWindowMinutes() != null) {
            validateAppealWindowMinutes(req.getAppealWindowMinutes());
        }

        Hackathon entity = hackathonMapper.toEntity(req);
        entity.setStatus(HackathonStatus.DRAFT);
        entity.setClonedFromHackathon(source);
        entity.setClonedAt(LocalDateTime.now());
        Integer uid = currentUserAccessor.currentUserId();
        if (uid != null) {
            entity.setCreatedBy(User.builder().id(uid).build());
        }
        Hackathon saved = hackathonRepository.save(entity);
        hackathonCloneSupport.copyStructureFrom(source, saved);

        Hackathon reloaded = hackathonRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", saved.getId()));
        HackathonResponse response = hackathonMapper.toResponse(reloaded);
        auditService.log(AuditAction.HACKATHON_CLONE, "hackathons", saved.getId(), Map.of(
                "sourceHackathonId", sourceId,
                "sourceHackathonName", source.getName(),
                "snapshot", response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public HackathonResponse getById(Integer id) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", id));
        return hackathonMapper.toResponse(h);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HackathonSummaryResponse> search(HackathonStatus status, Integer year,
                                                         Season season, String q, Pageable pageable) {
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        Page<Hackathon> page = hackathonRepository.search(status, year, season, keyword, pageable);
        List<HackathonSummaryResponse> items = page.getContent().stream()
                .map(hackathonMapper::toSummary)
                .toList();
        return PageResponse.from(page, items);
    }

    @Override
    public HackathonResponse update(Integer id, UpdateHackathonRequest req) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", id));
        archiveGuard.assertNotArchived(h);
        if (h.getStatus() != HackathonStatus.DRAFT) {
            throw new ConflictException(ErrorCode.HACKATHON_NOT_DRAFT,
                    "Chỉ được sửa Hackathon khi status=DRAFT (hiện %s)".formatted(h.getStatus()));
        }
        boolean identityChanged = !req.getName().equals(h.getName())
                || req.getSeason() != h.getSeason()
                || !req.getYear().equals(h.getYear());
        if (identityChanged
                && hackathonRepository.existsByNameAndSeasonAndYear(req.getName(), req.getSeason(), req.getYear())) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE,
                    "Hackathon đã tồn tại cho (name=%s, season=%s, year=%d)"
                            .formatted(req.getName(), req.getSeason(), req.getYear()));
        }
        if (!req.getSlug().equals(h.getSlug()) && hackathonRepository.existsBySlug(req.getSlug())) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE,
                    "Slug đã được sử dụng: " + req.getSlug());
        }
        validateEventStartAfterRegistrationEnd(req.getRegistrationEnd(), req.getEventStart());
        if (req.getAppealWindowMinutes() != null) {
            validateAppealWindowMinutes(req.getAppealWindowMinutes());
        }

        HackathonResponse before = hackathonMapper.toResponse(h);
        hackathonMapper.applyUpdate(h, req);
        Hackathon saved = hackathonRepository.save(h);
        HackathonResponse after = hackathonMapper.toResponse(saved);

        auditService.logBeforeAfter(AuditAction.HACKATHON_UPDATE, "hackathons", saved.getId(),
                before, after);
        return after;
    }

    @Override
    public Integer delete(Integer id) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", id));
        archiveGuard.assertNotArchived(h);
        if (h.getStatus() != HackathonStatus.DRAFT) {
            throw new ConflictException(ErrorCode.HACKATHON_NOT_DRAFT,
                    "Chỉ được xóa Hackathon khi status=DRAFT (hiện %s)".formatted(h.getStatus()));
        }
        if (trackRepository.existsByHackathonId(id)
                || roundRepository.existsByHackathon_Id(id)
                || eventRepository.existsByHackathonId(id)) {
            throw new ConflictException(ErrorCode.HACKATHON_HAS_CHILDREN,
                    "Hackathon còn Round/Track/Event con — không thể xóa");
        }

        HackathonResponse snapshot = hackathonMapper.toResponse(h);
        hackathonRepository.delete(h);
        auditService.log(AuditAction.HACKATHON_DELETE, "hackathons", id,
                Map.of("snapshot", snapshot));
        return id;
    }

    @Override
    public HackathonResponse uploadBanner(Integer id, MultipartFile file) {
        Hackathon hackathon = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", id));
        archiveGuard.assertNotArchived(hackathon);
        if (hackathon.getStatus() != HackathonStatus.DRAFT
                && hackathon.getStatus() != HackathonStatus.ONGOING) {
            throw new ConflictException(ErrorCode.HACKATHON_NOT_DRAFT,
                    "Chỉ được đổi banner khi status=DRAFT hoặc ONGOING (hiện %s)"
                            .formatted(hackathon.getStatus()));
        }
        String storageKey = bannerStorageService.store(hackathon.getId(), file, hackathon.getBannerUrl());
        hackathon.setBannerUrl(storageKey);
        Hackathon saved = hackathonRepository.save(hackathon);
        HackathonResponse response = hackathonMapper.toResponse(saved);
        auditService.log(AuditAction.HACKATHON_UPDATE, "hackathons", saved.getId(),
                Map.of("bannerUpdated", true));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getBannerResource(Integer id) {
        Hackathon hackathon = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", id));
        return bannerStorageService.loadAsResource(hackathon.getBannerUrl());
    }

    @Override
    public HackathonResponse updateAppealWindowMinutes(Integer id, Integer appealWindowMinutes) {
        Hackathon h = hackathonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", id));
        archiveGuard.assertNotArchived(h);
        if (h.getStatus() == HackathonStatus.FINISHED) {
            throw new ConflictException(ErrorCode.HACKATHON_ARCHIVED,
                    "Không sửa thời gian khiếu nại khi hackathon đã kết thúc");
        }
        if (h.getStatus() != HackathonStatus.DRAFT && h.getStatus() != HackathonStatus.ONGOING) {
            throw new ConflictException(ErrorCode.INVALID_STATE,
                    "Chỉ sửa thời gian khiếu nại khi DRAFT hoặc ONGOING (hiện %s)".formatted(h.getStatus()));
        }
        validateAppealWindowMinutes(appealWindowMinutes);

        boolean prelimPublished = roundRepository.findPreliminaryLikeByHackathonId(id).stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getIsPublished()));
        if (prelimPublished) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_LOCKED_AFTER_PUBLISH,
                    "Không sửa thời gian khiếu nại sau khi sơ loại đã công bố");
        }

        Integer before = h.getAppealWindowMinutes();
        h.setAppealWindowMinutes(appealWindowMinutes);
        Hackathon saved = hackathonRepository.save(h);
        HackathonResponse response = hackathonMapper.toResponse(saved);
        auditService.log(AuditAction.HACKATHON_APPEAL_WINDOW_UPDATE, "hackathons", saved.getId(),
                Map.of("before", before != null ? before : 30, "after", appealWindowMinutes));
        return response;
    }

    private void validateAppealWindowMinutes(int minutes) {
        if (minutes < 0) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Thời gian khiếu nại phải >= 0");
        }
        if (minutes > 0 && minutes < com.sealhackathon.api.appeals.service.AppealWindowService.MIN_APPEAL_WINDOW_MINUTES) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_BELOW_MINIMUM,
                    "Thời gian khiếu nại tối thiểu %d phút (hoặc 0 để tắt)"
                            .formatted(com.sealhackathon.api.appeals.service.AppealWindowService.MIN_APPEAL_WINDOW_MINUTES),
                    Map.of("min", com.sealhackathon.api.appeals.service.AppealWindowService.MIN_APPEAL_WINDOW_MINUTES,
                            "requested", minutes));
        }
    }

    private void validateEventStartAfterRegistrationEnd(java.time.LocalDate regEnd,
                                                        java.time.LocalDate eventStart) {
        if (regEnd != null && eventStart != null && eventStart.isBefore(regEnd)) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_DATE_RANGE,
                    "eventStart (%s) phải >= registrationEnd (%s)".formatted(eventStart, regEnd),
                    Map.of("eventStart", eventStart, "registrationEnd", regEnd));
        }
    }
}
