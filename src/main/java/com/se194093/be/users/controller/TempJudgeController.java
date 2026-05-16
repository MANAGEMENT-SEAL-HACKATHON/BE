package com.se194093.be.users.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.response.PageResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.users.dto.request.CreateTempJudgeRequest;
import com.se194093.be.users.dto.response.TempJudgeResponse;
import com.se194093.be.users.dto.response.UserSummaryResponse;
import com.se194093.be.users.service.TempJudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05a — Tạo / list Judge khách mời. Resend invitation tách ở {@link com.se194093.be.invitations.controller.InvitationController}.
 */
@RestController
@RequestMapping("/api/v1/users/temp-judges")
@RequiredArgsConstructor
@CoordinatorOnly
public class TempJudgeController {

    private final TempJudgeService tempJudgeService;

    @PostMapping
    public ResponseEntity<ApiResponse<TempJudgeResponse>> create(
            @Valid @RequestBody CreateTempJudgeRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(tempJudgeService.createTempJudge(req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> search(
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tempJudgeService.search(institution, q, pageable)));
    }
}
