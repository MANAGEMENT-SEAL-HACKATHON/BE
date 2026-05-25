package com.sealhackathon.api.hackathons.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.hackathons.dto.request.CreateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.request.UpdateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonSummaryResponse;
import com.sealhackathon.api.hackathons.service.HackathonService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * FR-01 — REST controller cho Hackathon CRUD.
 *
 * <p>Spec: {@code docs/mf01/api/fr-01-hackathons.md}.
 * <p>State transition (FR-06) ở {@link HackathonStatusController}.
 */
@Tag(name = "Hackathon", description = "FR-01 — CRUD hackathon")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
@CoordinatorOnly
public class HackathonController {

    private final HackathonService hackathonService;

    @PostMapping
    @Operation(summary = "Tạo hackathon mới", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<HackathonResponse>> create(@Valid @RequestBody CreateHackathonRequest req) {
        HackathonResponse data = hackathonService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm và phân trang hackathon", description = "Các tham số truy vấn đều tùy chọn, nếu không cung cấp sẽ trả về tất cả hackathon.")
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
    @Operation(summary = "Lấy thông tin chi tiết hackathon theo ID", description = "Trả về lỗi 404 nếu không tìm thấy hackathon với ID đã cho.")
    public ResponseEntity<ApiResponse<HackathonResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy hackathon với ID đã cho.")
    public ResponseEntity<ApiResponse<HackathonResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateHackathonRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy hackathon với ID đã cho.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = hackathonService.delete(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
