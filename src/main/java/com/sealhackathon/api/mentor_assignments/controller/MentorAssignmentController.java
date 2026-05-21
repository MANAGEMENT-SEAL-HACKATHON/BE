package com.sealhackathon.api.mentor_assignments.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.mentor_assignments.dto.request.CreateMentorAssignmentRequest;
import com.sealhackathon.api.mentor_assignments.dto.response.MentorAssignmentResponse;
import com.sealhackathon.api.mentor_assignments.service.MentorAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * FR-05b — Mentor Assignment (POST/DELETE + 2 GET inverse-lookup).
 */
@Tag(name = "Personnel — Mentor", description = "FR-05b — Phân công mentor vào track")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class MentorAssignmentController {

    private final MentorAssignmentService mentorAssignmentService;

    @PostMapping("/api/v1/mentor-assignments")
    @Operation(summary = "Phân công mentor vào track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<MentorAssignmentResponse>> assign(
            @Valid @RequestBody CreateMentorAssignmentRequest req
    ) {
        MentorAssignmentService.CreateResult result = mentorAssignmentService.assign(req);
        List<Warning> warnings = result.conflictWarning()
                .map(List::of).orElse(List.of());
        return ResponseEntity.status(201).body(
                ApiResponse.createdWithWarnings(result.assignment(), warnings)
        );
    }

    @GetMapping("/api/v1/tracks/{trackId}/mentors")
    @Operation(summary = "Liệt kê mentor của track", description = "Không dùng ở GĐ1 — xem runbook Bước 5.")
    public ResponseEntity<ApiResponse<List<MentorAssignmentResponse>>> listByTrack(@PathVariable Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorAssignmentService.listByTrack(trackId)));
    }

    @GetMapping("/api/v1/users/{mentorId}/track-assignments")
    @Operation(summary = "Liệt kê track mà mentor được phân công", description = "Không dùng ở GĐ1 — xem runbook Bước 5.")
    public ResponseEntity<ApiResponse<List<MentorAssignmentResponse>>> listByMentor(@PathVariable Integer mentorId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorAssignmentService.listByMentor(mentorId)));
    }

    @DeleteMapping("/api/v1/mentor-assignments/{id}")
    @Operation(summary = "Hủy phân công mentor khỏi track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unassign(@PathVariable Integer id) {
        Integer deletedId = mentorAssignmentService.unassign(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Unassigned"));
    }
}
