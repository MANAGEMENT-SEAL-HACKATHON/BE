package com.se194093.be.tracks.service;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.value_object.TrackStatus;

import java.util.List;

/**
 * FR-02 — CRUD Track.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Hackathon parent phải ở status DRAFT hoặc ONGOING khi tạo/sửa Track
 *       (vi phạm trả 409 {@code TRACK_HACKATHON_LOCKED}).</li>
 *   <li>{@code maxTeamSize >= minTeamSize} → 422 {@code TRACK_INVALID_TEAM_SIZE}.</li>
 *   <li>{@code maxTeamsPerGroup <= maxTeams} nếu cả hai có giá trị → 422 {@code TRACK_INVALID_GROUP_CAP}.</li>
 *   <li>DELETE guard:
 *     <ul>
 *       <li>còn team {@code registration_track_id=id} với status PENDING/ACTIVE → 409 {@code TRACK_HAS_TEAMS}</li>
 *       <li>còn round {@code is_active=TRUE} → 409 {@code TRACK_HAS_ACTIVE_ROUND}</li>
 *     </ul>
 *   </li>
 *   <li>Update sang CANCELLED khi còn team registered → response 200 kèm
 *       {@code warnings:[{code:"TRACK_CANCELLED_HAS_TEAMS"}]} (không block).</li>
 * </ul>
 *
 * <p>Audit: {@code TRACK_CREATE}, {@code TRACK_UPDATE}, {@code TRACK_DELETE}.
 */
public interface TrackService {

    TrackResponse create(Integer hackathonId, CreateTrackRequest req);

    List<TrackSummaryResponse> listByHackathon(Integer hackathonId, TrackStatus statusFilter);

    TrackResponse getById(Integer id);

    /**
     * @return cả response và warnings (vd CANCELLED còn team) — controller bọc qua {@link ApiResponse}.
     */
    UpdateResult update(Integer id, UpdateTrackRequest req);

    Integer delete(Integer id);

    /**
     * Container nhỏ trả về cả entity response và list warnings.
     */
    record UpdateResult(TrackResponse track, java.util.List<com.se194093.be.common.response.Warning> warnings) {}
}
