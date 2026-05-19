package com.se194093.be.tracks.service;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.value_object.TrackStatus;

import java.util.List;

/**
 * FR-03 — CRUD Track trong Round Sơ loại.
 */
public interface TrackService {

    TrackResponse createByRound(Integer roundId, CreateTrackRequest req);

    /** @deprecated delegate — cần round PRELIMINARY đầu tiên của hackathon */
    @Deprecated
    TrackResponse create(Integer hackathonId, CreateTrackRequest req);

    List<TrackSummaryResponse> listByHackathon(Integer hackathonId, TrackStatus statusFilter);

    TrackResponse getById(Integer id);

    UpdateResult update(Integer id, UpdateTrackRequest req);

    Integer delete(Integer id);

    record UpdateResult(TrackResponse track, java.util.List<com.se194093.be.common.response.Warning> warnings) {}
}
