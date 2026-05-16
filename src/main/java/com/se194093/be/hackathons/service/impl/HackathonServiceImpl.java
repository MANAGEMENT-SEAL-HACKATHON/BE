package com.se194093.be.hackathons.service.impl;

import com.se194093.be.common.response.PageResponse;
import com.se194093.be.hackathons.dto.request.CreateHackathonRequest;
import com.se194093.be.hackathons.dto.request.UpdateHackathonRequest;
import com.se194093.be.hackathons.dto.response.HackathonResponse;
import com.se194093.be.hackathons.dto.response.HackathonSummaryResponse;
import com.se194093.be.hackathons.service.HackathonService;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.hackathons.value_object.Season;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Skeleton impl cho {@link HackathonService}.
 *
 * <p>TẦNG NGHIỆP VỤ — chưa implement. Dev triển khai theo pseudocode trong
 * {@code docs/api/mf-01/fr-01-hackathons.md} với các bước:
 * <ol>
 *   <li>Validate UNIQUE name/season/year và slug → throw ConflictException</li>
 *   <li>Validate eventStart &gt;= registrationEnd → throw BusinessRuleException</li>
 *   <li>Map DTO → entity; set status=DRAFT; set createdBy từ {@code CurrentUserAccessor}</li>
 *   <li>Save entity; gọi {@code AuditService.log(HACKATHON_CREATE, ...)} cùng transaction</li>
 *   <li>Trả response map từ entity vừa save</li>
 * </ol>
 *
 * <p>Inject sẵn: {@code HackathonRepository}, {@code HackathonMapper}, {@code AuditService},
 * {@code CurrentUserAccessor} — KHÔNG inject ở skeleton để tránh warning bean rỗng; Dev tự bổ sung.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HackathonServiceImpl implements HackathonService {

    // TODO Dev: inject HackathonRepository, HackathonMapper, AuditService, CurrentUserAccessor,
    //          TrackRepository, RoundRepository, EventRepository (cho guard DELETE)

    @Override
    public HackathonResponse create(CreateHackathonRequest req) {
        // TODO Dev: implement theo pseudocode FR-01 §1
        throw new UnsupportedOperationException("FR-01 POST /hackathons - to be implemented");
    }

    @Override
    public HackathonResponse getById(Integer id) {
        // TODO Dev: findById hoặc throw ResourceNotFoundException("Hackathon", id)
        throw new UnsupportedOperationException("FR-01 GET /hackathons/{id} - to be implemented");
    }

    @Override
    public PageResponse<HackathonSummaryResponse> search(HackathonStatus status, Integer year,
                                                         Season season, String q, Pageable pageable) {
        // TODO Dev: repository.search(...) → map sang HackathonSummaryResponse → PageResponse.from()
        throw new UnsupportedOperationException("FR-01 GET /hackathons - to be implemented");
    }

    @Override
    public HackathonResponse update(Integer id, UpdateHackathonRequest req) {
        // TODO Dev:
        //  - findById → 404 nếu thiếu
        //  - guard status = DRAFT → 409 HACKATHON_NOT_DRAFT
        //  - re-validate UNIQUE nếu name/season/year/slug đổi
        //  - validate eventStart >= registrationEnd
        //  - mapper.applyUpdate(entity, req); save; audit HACKATHON_UPDATE { before, after }
        throw new UnsupportedOperationException("FR-01 PUT /hackathons/{id} - to be implemented");
    }

    @Override
    public Integer delete(Integer id) {
        // TODO Dev:
        //  - findById → 404
        //  - guard status = DRAFT → 409 HACKATHON_NOT_DRAFT
        //  - guard !exists Track/Round/Event của hackathon → 409 HACKATHON_HAS_CHILDREN
        //  - delete; audit HACKATHON_DELETE
        throw new UnsupportedOperationException("FR-01 DELETE /hackathons/{id} - to be implemented");
    }
}
