package com.se194093.be.hackathons.service;

import com.se194093.be.common.response.PageResponse;
import com.se194093.be.hackathons.dto.request.CreateHackathonRequest;
import com.se194093.be.hackathons.dto.request.UpdateHackathonRequest;
import com.se194093.be.hackathons.dto.response.HackathonResponse;
import com.se194093.be.hackathons.dto.response.HackathonSummaryResponse;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.hackathons.value_object.Season;
import org.springframework.data.domain.Pageable;

/**
 * FR-01 — CRUD Hackathon.
 *
 * <p>Business rules tổng hợp:
 * <ul>
 *   <li>UNIQUE(name, season, year); UNIQUE(slug) — vi phạm trả 409 {@code HACKATHON_DUPLICATE}.</li>
 *   <li>Status mặc định DRAFT khi tạo; KHÔNG nhận từ client.</li>
 *   <li>{@code eventStart} phải &gt;= {@code registrationEnd} — vi phạm trả 422 {@code HACKATHON_DATE_RANGE}.</li>
 *   <li>PUT chỉ cho phép khi {@code status = DRAFT} (trả 409 {@code HACKATHON_NOT_DRAFT}).</li>
 *   <li>DELETE chỉ khi DRAFT và chưa có Track/Round/Event con (trả 409 {@code HACKATHON_HAS_CHILDREN}).</li>
 * </ul>
 *
 * <p>Audit actions phát ra: {@code HACKATHON_CREATE}, {@code HACKATHON_UPDATE}, {@code HACKATHON_DELETE}.
 */
public interface HackathonService {

    /**
     * POST /api/v1/hackathons.
     *
     * @throws com.se194093.be.common.exception.ConflictException UNIQUE violation
     * @throws com.se194093.be.common.exception.BusinessRuleException date logic invalid
     */
    HackathonResponse create(CreateHackathonRequest req);

    /**
     * GET /api/v1/hackathons/{id}.
     *
     * @throws com.se194093.be.common.exception.ResourceNotFoundException
     */
    HackathonResponse getById(Integer id);

    /**
     * GET /api/v1/hackathons (filter + paging).
     */
    PageResponse<HackathonSummaryResponse> search(HackathonStatus status, Integer year, Season season,
                                                  String q, Pageable pageable);

    /**
     * PUT /api/v1/hackathons/{id}.
     *
     * @throws com.se194093.be.common.exception.ConflictException        status ≠ DRAFT hoặc UNIQUE đổi trùng
     * @throws com.se194093.be.common.exception.BusinessRuleException    date logic invalid
     * @throws com.se194093.be.common.exception.ResourceNotFoundException
     */
    HackathonResponse update(Integer id, UpdateHackathonRequest req);

    /**
     * DELETE /api/v1/hackathons/{id}.
     *
     * @throws com.se194093.be.common.exception.ConflictException status ≠ DRAFT hoặc còn children
     */
    Integer delete(Integer id);
}
