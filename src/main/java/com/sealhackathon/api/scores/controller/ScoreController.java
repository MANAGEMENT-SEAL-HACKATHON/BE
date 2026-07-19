package com.sealhackathon.api.scores.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.JudgeOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.scores.dto.request.SubmitScoreRequest;
import com.sealhackathon.api.scores.dto.response.ScoreResponse;
import com.sealhackathon.api.scores.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Scores (GĐ3-GĐ5)", description = "FR-18/18A — Chấm điểm")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @PostMapping
    @JudgeOnly
    @Operation(summary = "FR-18/18A — Judge/Mentor (đã phân công) chấm điểm bài nộp (upsert nháp)")
    public ResponseEntity<ApiResponse<ScoreResponse>> submit(@Valid @RequestBody SubmitScoreRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(scoreService.submitScore(req)));
    }
}
