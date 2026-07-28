package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.response.*;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import com.sealhackathon.api.teams.dto.request.CreateTeamRequest;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Student Portal", description = "FR-U — Portal sinh viên /api/v1/me/*")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@StudentOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class StudentMeController {

    private final StudentPortalService studentPortalService;

    @GetMapping("/teams")
    @Operation(summary = "FR-U-15 — Đội của tôi",
            description = "Mặc định chỉ PENDING/ACTIVE. `includeEliminated=true` thêm đội ELIMINATED (trang kết quả).")
    public ResponseEntity<ApiResponse<List<MeTeamSummaryResponse>>> listMyTeams(
            @RequestParam(defaultValue = "false") boolean includeEliminated) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.listMyTeams(includeEliminated)));
    }

    @PostMapping("/teams")
    @Operation(summary = "FR-U-07 — Leader tạo đội (portal alias của POST /teams)")
    public ResponseEntity<ApiResponse<TeamResponse>> createMyTeam(@Valid @RequestBody CreateTeamRequest req) {
        TeamResponse data = studentPortalService.createTeam(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping("/prizes")
    @Operation(summary = "FR-U-28 — Giải thưởng của tôi")
    public ResponseEntity<ApiResponse<List<StudentPrizeResponse>>> listMyPrizes() {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.listMyPrizes()));
    }

    @GetMapping("/certificates")
    @Operation(summary = "FR-U-29 — Danh sách chứng nhận")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> listCertificates() {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.listMyCertificates()));
    }

    @GetMapping("/certificates/{id}/download")
    @Operation(summary = "FR-U-29 — Xem hoặc tải chứng nhận PDF",
            description = "Mặc định `inline` (xem trên trình duyệt). `?download=true` → `attachment` (tải file).")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean download) throws java.io.IOException {
        CertificateDownload file = studentPortalService.getCertificateDownload(id);
        byte[] bytes = file.readBytes();
        String disposition = download ? "attachment" : "inline";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/appeals")
    @Operation(summary = "FR-U-30 — Gửi khiếu nại")
    public ResponseEntity<ApiResponse<AppealResponse>> createAppeal(@Valid @RequestBody CreateAppealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(studentPortalService.createAppeal(request)));
    }

    @GetMapping("/history")
    @Operation(summary = "FR-U-31 — Lịch sử tham gia")
    public ResponseEntity<ApiResponse<StudentHistoryResponse>> getHistory() {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getHistory()));
    }

    @GetMapping("/annual-awards")
    @Operation(summary = "FR-U-32 — Giải cá nhân năm (fall)")
    public ResponseEntity<ApiResponse<List<AnnualAwardResponse>>> getAnnualAwards(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getAnnualAwards(year)));
    }
}
