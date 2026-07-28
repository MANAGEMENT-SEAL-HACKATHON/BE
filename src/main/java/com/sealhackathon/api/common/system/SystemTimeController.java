package com.sealhackathon.api.common.system;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Tag(name = "System")
@RestController
@RequestMapping("/api/v1/system")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class SystemTimeController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @GetMapping("/time")
    @ApprovedOnly
    @Operation(summary = "Server clock — FE sync (serverNow)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> serverTime() {
        Instant instant = Instant.now();
        LocalDateTime local = LocalDateTime.ofInstant(instant, ZONE);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "serverNow", local.toString(),
                "serverNowEpochMs", instant.toEpochMilli(),
                "zone", ZONE.getId()
        )));
    }
}
