package com.sealhackathon.api.announcements.controller;

import com.sealhackathon.api.announcements.entity.HackathonAnnouncement;
import com.sealhackathon.api.announcements.service.AnnouncementService;
import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Announcements")
@RestController
@RequestMapping("/api/v1/hackathons/{hackathonId}/announcements")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @ApprovedOnly
    @Operation(summary = "Announcement center feed + unread")
    public ResponseEntity<ApiResponse<Map<String, Object>>> feed(@PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.feedForCurrentUser(hackathonId)));
    }

    @PostMapping("/viewed")
    @ApprovedOnly
    @Operation(summary = "Mark announcements viewed (lastViewedAt)")
    public ResponseEntity<ApiResponse<Void>> markViewed(@PathVariable Integer hackathonId) {
        announcementService.markViewed(hackathonId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/{announcementId}/soft-hide")
    @CoordinatorOnly
    @Operation(summary = "Soft-hide announcement from student default feed")
    public ResponseEntity<ApiResponse<HackathonAnnouncement>> softHide(
            @PathVariable Integer hackathonId,
            @PathVariable Integer announcementId,
            @RequestParam(defaultValue = "true") boolean hidden) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.softHide(announcementId, hidden)));
    }
}
