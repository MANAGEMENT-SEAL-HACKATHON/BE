package com.sealhackathon.api.hackathons.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.hackathons.dto.request.CreateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.request.HackathonLotteryRequest;
import com.sealhackathon.api.hackathons.dto.request.UpdateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.CloseRegistrationEarlyResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonLotteryResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonSummaryResponse;
import com.sealhackathon.api.hackathons.service.HackathonLotteryService;
import com.sealhackathon.api.hackathons.service.HackathonRegistrationCloseService;
import com.sealhackathon.api.hackathons.service.HackathonService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.core.io.Resource;

import java.net.URI;
import java.util.Map;

/**
 * FR-01 — REST controller cho Hackathon CRUD.
 */
@Tag(name = "Hackathon", description = "FR-01 — CRUD hackathon")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
// ĐÃ GỠ BỎ @CoordinatorOnly Ở ĐÂY ĐỂ MỞ CỬA CHO TẤT CẢ USER ĐƯỢC XEM DANH SÁCH
public class HackathonController {

    private final HackathonService hackathonService;
    private final HackathonLotteryService hackathonLotteryService;
    private final HackathonRegistrationCloseService hackathonRegistrationCloseService;

    // API MỚI DÀNH RIÊNG CHO FRONTEND SET MẶC ĐỊNH
    @GetMapping("/active")
    @Operation(summary = "Lấy danh sách Hackathon ĐANG DIỄN RA", description = "Mọi user (Student/Judge) đều gọi được để FE set mặc định sự kiện hiện tại.")
    public ResponseEntity<ApiResponse<PageResponse<HackathonSummaryResponse>>> getActiveHackathons(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        // Tận dụng hàm search có sẵn, ép cứng status = ONGOING để FE không cần truyền param
        return ResponseEntity.ok(ApiResponse.ok(
                hackathonService.search(HackathonStatus.ONGOING, null, null, null, pageable)
        ));
    }

    @PostMapping
    @CoordinatorOnly // GẮN BẢO VỆ VÀO TỪNG HÀM
    @Operation(summary = "Tạo hackathon mới", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<HackathonResponse>> create(@Valid @RequestBody CreateHackathonRequest req) {
        HackathonResponse data = hackathonService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @PostMapping("/{id}/clone")
    @CoordinatorOnly
    @Operation(summary = "Nhân bản hackathon", description = "Sao chép rounds/tracks/criteria từ sự kiện nguồn. Không copy teams/judges/events.")
    public ResponseEntity<ApiResponse<HackathonResponse>> clone(
            @PathVariable Integer id,
            @Valid @RequestBody CreateHackathonRequest req) {
        HackathonResponse data = hackathonService.cloneFrom(id, req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/hackathons/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm và phân trang hackathon", description = "Mọi user đều có thể xem danh sách.")
    public ResponseEntity<ApiResponse<PageResponse<HackathonSummaryResponse>>> search(
            @RequestParam(required = false) HackathonStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Season season,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.search(status, year, season, q, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết hackathon theo ID", description = "Mọi user đều có thể xem chi tiết sự kiện.")
    public ResponseEntity<ApiResponse<HackathonResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.getById(id)));
    }

    @PutMapping("/{id}")
    @CoordinatorOnly // GẮN BẢO VỆ
    @Operation(summary = "Cập nhật thông tin hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<HackathonResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateHackathonRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @CoordinatorOnly // GẮN BẢO VỆ
    @Operation(summary = "Xóa hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = hackathonService.delete(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }

    @PatchMapping("/{hackathonId}/lottery")
    @CoordinatorOnly // GẮN BẢO VỆ
    @Operation(summary = "FR-13B — Bốc thăm Track (batch)")
    public ResponseEntity<ApiResponse<HackathonLotteryResponse>> lottery(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody HackathonLotteryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                hackathonLotteryService.runLottery(hackathonId, req)));
    }

    @PostMapping("/{id}/close-registration-early")
    @CoordinatorOnly
    @Operation(summary = "Kết thúc đăng ký sớm — khóa đội ACTIVE, loại đội/thí sinh không đủ điều kiện")
    public ResponseEntity<ApiResponse<CloseRegistrationEarlyResponse>> closeRegistrationEarly(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonRegistrationCloseService.closeRegistrationEarly(id)));
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CoordinatorOnly
    @Operation(summary = "Upload ảnh banner hackathon (DRAFT hoặc ONGOING)")
    public ResponseEntity<ApiResponse<HackathonResponse>> uploadBanner(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.uploadBanner(id, file)));
    }

    @GetMapping("/{id}/banner")
    @Operation(summary = "Tải ảnh banner hackathon")
    public ResponseEntity<Resource> getBanner(@PathVariable Integer id) {
        Resource resource = hackathonService.getBannerResource(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"banner\"")
                .body(resource);
    }
}