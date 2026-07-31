package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.dto.request.CreateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.request.UpdateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonSummaryResponse;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

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
     * @throws ConflictException UNIQUE violation
     * @throws BusinessRuleException date logic invalid
     */
    HackathonResponse create(CreateHackathonRequest req);

    /**
     * POST /api/v1/hackathons/{id}/clone — nhân bản rounds/tracks/criteria từ nguồn.
     */
    HackathonResponse cloneFrom(Integer sourceId, CreateHackathonRequest req);

    /**
     * GET /api/v1/hackathons/{id}.
     *
     * @throws ResourceNotFoundException
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
     * @throws ConflictException        status ≠ DRAFT hoặc UNIQUE đổi trùng
     * @throws BusinessRuleException    date logic invalid
     * @throws ResourceNotFoundException
     */
    HackathonResponse update(Integer id, UpdateHackathonRequest req);

    /**
     * DELETE /api/v1/hackathons/{id}.
     *
     * @throws ConflictException status ≠ DRAFT hoặc còn children
     */
    Integer delete(Integer id);

    HackathonResponse uploadBanner(Integer id, MultipartFile file);

    Resource getBannerResource(Integer id);
}
