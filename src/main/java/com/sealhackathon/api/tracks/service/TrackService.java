package com.sealhackathon.api.tracks.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.tracks.dto.request.CreateTrackRequest;
import com.sealhackathon.api.tracks.dto.request.UpdateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * FR-03 — CRUD Track trong Round Sơ loại.
 */
public interface TrackService {

    TrackResponse createByRound(Integer roundId, CreateTrackRequest req);

    List<TrackSummaryResponse> listByHackathon(Integer hackathonId, TrackStatus statusFilter);

    List<TrackSummaryResponse> listByRound(Integer roundId, TrackStatus statusFilter);

    TrackResponse getById(Integer id);

    UpdateResult update(Integer id, UpdateTrackRequest req);

    Integer delete(Integer id);

    TrackResponse uploadProblemStatement(Integer id, MultipartFile file);

    Resource downloadProblemStatement(Integer id);

    record UpdateResult(TrackResponse track, java.util.List<Warning> warnings) {}
}
