package com.sealhackathon.api.users.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.users.dto.response.UserInviteLookupResponse;
import com.sealhackathon.api.users.service.UserLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Users — Lookup", description = "Tìm tài khoản sinh viên để mời vào đội")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserLookupController {

    private final UserLookupService userLookupService;

    @GetMapping("/lookup")
    @ApprovedOnly
    @Operation(summary = "Tìm sinh viên theo email / tên / mã SV (mời đội)")
    public ResponseEntity<ApiResponse<List<UserInviteLookupResponse>>> lookup(
            @RequestParam("q") String q) {
        return ResponseEntity.ok(ApiResponse.ok(userLookupService.lookupInviteCandidates(q)));
    }

    @GetMapping("/lookup/coordinator")
    @CoordinatorOnly
    @Operation(summary = "Tìm giám khảo/mentor theo email / tên (mời nhân sự)")
    public ResponseEntity<ApiResponse<List<UserInviteLookupResponse>>> lookupCoordinator(
            @RequestParam("q") String q) {
        return ResponseEntity.ok(ApiResponse.ok(userLookupService.lookupCoordinatorInviteCandidates(q)));
    }
}
