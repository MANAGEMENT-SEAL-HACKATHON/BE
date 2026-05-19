package com.se194093.be.tracks.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.service.TrackService;
import com.se194093.be.tracks.value_object.TrackStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * FR-02 — Track controller.
 *
 * <p>Routes:
 * <ul>
 *   <li>POST/GET nested theo Hackathon parent: {@code /hackathons/{hackathonId}/tracks}</li>
 *   <li>GET/PUT/DELETE single: {@code /tracks/{id}}</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class TrackController {

    private final TrackService trackService;

    // ------- nested ROUTES -------

    @PostMapping("/api/v1/rounds/{roundId}/tracks")
    public ResponseEntity<ApiResponse<TrackResponse>> createByRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody CreateTrackRequest req
    ) {
        TrackResponse data = trackService.createByRound(roundId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tracks/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    /** @deprecated delegate — tạo Track trong Round Sơ loại đầu tiên */
    @Deprecated
    @PostMapping("/api/v1/hackathons/{hackathonId}/tracks")
    public ResponseEntity<ApiResponse<TrackResponse>> createLegacy(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody CreateTrackRequest req
    ) {
        TrackResponse data = trackService.create(hackathonId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tracks/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping("/api/v1/hackathons/{hackathonId}/tracks")
    public ResponseEntity<ApiResponse<List<TrackSummaryResponse>>> listByHackathon(
            @PathVariable Integer hackathonId,
            @RequestParam(required = false) TrackStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(trackService.listByHackathon(hackathonId, status)));
    }

    // ------- single resource ROUTES -------

    @GetMapping("/api/v1/tracks/{id}")
    public ResponseEntity<ApiResponse<TrackResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(trackService.getById(id)));
    }

    @PutMapping("/api/v1/tracks/{id}")
    public ResponseEntity<ApiResponse<TrackResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTrackRequest req
    ) {
        TrackService.UpdateResult result = trackService.update(id, req);
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.track(), result.warnings()));
    }

    @DeleteMapping("/api/v1/tracks/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = trackService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
