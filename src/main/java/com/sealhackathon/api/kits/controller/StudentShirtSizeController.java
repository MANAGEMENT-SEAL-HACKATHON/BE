package com.sealhackathon.api.kits.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.kits.dto.request.UpdateShirtSizeRequest;
import com.sealhackathon.api.kits.dto.response.ShirtSizeResponse;
import com.sealhackathon.api.kits.service.KitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Kits — Student shirt size", description = "Sinh viên khai size + dáng áo trên đăng ký hackathon")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@StudentOnly
@ConditionalOnProperty(name = "app.kits.enabled", havingValue = "true", matchIfMissing = true)
public class StudentShirtSizeController {

    private final KitService kitService;

    @GetMapping("/shirt-sizes")
    @Operation(summary = "Size/dáng áo trên mọi đăng ký hackathon của tôi")
    public ResponseEntity<ApiResponse<List<ShirtSizeResponse>>> listMine() {
        return ResponseEntity.ok(ApiResponse.ok(kitService.listMyShirtSizes()));
    }

    @PutMapping("/shirt-size")
    @Operation(summary = "Cập nhật preferredShirtSize (+ fit, mặc định UNISEX) trên mọi đăng ký (onboarding)")
    public ResponseEntity<ApiResponse<List<ShirtSizeResponse>>> updateAll(
            @Valid @RequestBody UpdateShirtSizeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.updateMyShirtSizeAll(req)));
    }

    @PutMapping("/hackathons/{hackathonId}/shirt-size")
    @Operation(summary = "Cập nhật preferredShirtSize (+ fit) cho một hackathon")
    public ResponseEntity<ApiResponse<ShirtSizeResponse>> updateOne(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody UpdateShirtSizeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.updateMyShirtSize(hackathonId, req)));
    }
}
