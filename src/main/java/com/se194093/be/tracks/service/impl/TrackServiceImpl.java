package com.se194093.be.tracks.service.impl;

import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.service.TrackService;
import com.se194093.be.tracks.value_object.TrackStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-02-tracks.md}.
 *
 * <p>Inject: TrackRepository, HackathonRepository (validate parent status), TrackMapper,
 * AuditService, plus repository team/round/mentor cho guard delete.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrackServiceImpl implements TrackService {

    @Override
    public TrackResponse create(Integer hackathonId, CreateTrackRequest req) {
        // TODO Dev:
        //  1. findHackathon(hackathonId) or throw ResourceNotFoundException
        //  2. guard hackathon.status IN (DRAFT, ONGOING) else throw ConflictException(TRACK_HACKATHON_LOCKED)
        //  3. validate maxTeamSize >= minTeamSize → 422 TRACK_INVALID_TEAM_SIZE
        //  4. validate maxTeamsPerGroup <= maxTeams (nếu cả hai có) → 422 TRACK_INVALID_GROUP_CAP
        //  5. save; audit TRACK_CREATE
        throw new UnsupportedOperationException("FR-02 POST /tracks - to be implemented");
    }

    @Override
    public List<TrackSummaryResponse> listByHackathon(Integer hackathonId, TrackStatus statusFilter) {
        // TODO Dev: repo.findByHackathonIdOrderById(...) hoặc findByHackathonIdAndStatus(...) khi có filter
        throw new UnsupportedOperationException("FR-02 GET /tracks - to be implemented");
    }

    @Override
    public TrackResponse getById(Integer id) {
        throw new UnsupportedOperationException("FR-02 GET /tracks/{id} - to be implemented");
    }

    @Override
    public UpdateResult update(Integer id, UpdateTrackRequest req) {
        // TODO Dev:
        //  - validate parent hackathon status; validate size/group cap
        //  - applyUpdate; save
        //  - nếu req.status == CANCELLED && có team status PENDING/ACTIVE → emit warning TRACK_CANCELLED_HAS_TEAMS
        //  - audit TRACK_UPDATE { before, after }
        throw new UnsupportedOperationException("FR-02 PUT /tracks/{id} - to be implemented");
    }

    @Override
    public Integer delete(Integer id) {
        // TODO Dev:
        //  - findById → 404
        //  - guard: countTeamsPendingActive(id) == 0 else 409 TRACK_HAS_TEAMS
        //  - guard: !existsActiveRound(id) else 409 TRACK_HAS_ACTIVE_ROUND
        //  - guard: hackathon.status IN (DRAFT, ONGOING) else 409 TRACK_HACKATHON_LOCKED
        //  - delete (cascade rounds/mentor/judge/criteria from DB)
        //  - notify mentor bị hủy phân công
        //  - audit TRACK_DELETE
        throw new UnsupportedOperationException("FR-02 DELETE /tracks/{id} - to be implemented");
    }
}
