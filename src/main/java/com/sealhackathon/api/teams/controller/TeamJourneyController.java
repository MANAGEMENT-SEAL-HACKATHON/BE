package com.sealhackathon.api.teams.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.AuthenticatedOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.teams.dto.response.TeamJourneyResponse;
import com.sealhackathon.api.teams.service.TeamJourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Teams Journey (GĐ3-GĐ5)", description = "Hành trình đội qua các round")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamJourneyController {

    private final TeamJourneyService teamJourneyService;

    @GetMapping("/{teamId}/journey")
    @AuthenticatedOnly
    @Operation(summary = "Lấy hành trình đội qua các round")
    public ResponseEntity<ApiResponse<TeamJourneyResponse>> getJourney(@PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamJourneyService.getJourney(teamId)));
    }
}
