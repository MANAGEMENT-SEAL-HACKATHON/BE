package com.se194093.be.hackathons.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.PageResponse;
import com.se194093.be.common.security.CurrentUserAccessor;
import com.se194093.be.events.repository.EventRepository;
import com.se194093.be.hackathons.dto.request.CreateHackathonRequest;
import com.se194093.be.hackathons.dto.request.UpdateHackathonRequest;
import com.se194093.be.hackathons.dto.response.HackathonResponse;
import com.se194093.be.hackathons.dto.response.HackathonSummaryResponse;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.mapper.HackathonMapper;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.service.HackathonService;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.hackathons.value_object.Season;
import com.se194093.be.tracks.repository.TrackRepository;
import com.se194093.be.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EventRepository eventRepository;

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
        if (h.getStatus() != HackathonStatus.DRAFT) {
            throw new ConflictException(ErrorCode.HACKATHON_NOT_DRAFT,
                    "Chỉ được xóa Hackathon khi status=DRAFT (hiện %s)".formatted(h.getStatus()));
        }
        if (trackRepository.existsByHackathonId(id) || eventRepository.existsByHackathonId(id)) {
            throw new ConflictException(ErrorCode.HACKATHON_HAS_CHILDREN,
                    "Hackathon còn Track/Event con — không thể xóa");
        }

        HackathonResponse snapshot = hackathonMapper.toResponse(h);
        hackathonRepository.delete(h);
        auditService.log(AuditAction.HACKATHON_DELETE, "hackathons", id,
                Map.of("snapshot", snapshot));
        return id;
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
