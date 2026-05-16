package com.se194093.be.invitations.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.invitations.service.InvitationService;
import com.se194093.be.users.dto.response.TempJudgeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05a — Resend invitation cho Judge khách mời.
 */
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@CoordinatorOnly
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/{id}/resend")
    public ResponseEntity<ApiResponse<TempJudgeResponse.InvitationInfo>> resend(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.resend(id), "Invitation resent"));
    }
}
