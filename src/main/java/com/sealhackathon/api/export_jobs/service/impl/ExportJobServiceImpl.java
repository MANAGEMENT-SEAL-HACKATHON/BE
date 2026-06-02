package com.sealhackathon.api.export_jobs.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.export_jobs.dto.request.CreateExportJobRequest;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;
import com.sealhackathon.api.export_jobs.entity.ExportJob;
import com.sealhackathon.api.export_jobs.repository.ExportJobRepository;
import com.sealhackathon.api.export_jobs.service.ExportJobService;
import com.sealhackathon.api.export_jobs.value_object.ExportJobStatus;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExportJobServiceImpl implements ExportJobService {

    private final ExportJobRepository exportJobRepository;
    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;

    @Override
    public ExportJobResponse create(Integer hackathonId, CreateExportJobRequest req) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        // Rào chắn: Chỉ cho phép xuất dữ liệu khi Hackathon đã kết thúc (FINISHED)
        if (hackathon.getStatus() != HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ có thể xuất báo cáo hệ thống khi Hackathon đã chính thức đóng sổ (FINISHED).");
        }

        User requester = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));

        ExportJob job = ExportJob.builder()
                .hackathon(hackathon)
                .type(req.getType())
                .status(ExportJobStatus.PENDING)
                .requestedBy(requester)
                .createdAt(LocalDateTime.now())
                .build();

        ExportJob saved = exportJobRepository.save(job);

        // Mô phỏng việc gửi job vào Message Queue để xử lý bất đồng bộ
        log.info("[ExportJob] Đã đưa yêu cầu xuất báo cáo {} vào hàng đợi. JobID: {}", req.getType(), saved.getId());

        // -------------------------------------------------------------------------
        // GIẢ LẬP WORKER: Tự động hoàn thành Job ngay lập tức để FE dễ dàng test
        // -------------------------------------------------------------------------
        saved.setStatus(ExportJobStatus.DONE);
        saved.setFileUrl("https://seal-storage.s3.ap-southeast-1.amazonaws.com/exports/hackathon_"
                + hackathonId + "_" + req.getType().name() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".csv");
        saved.setFinishedAt(LocalDateTime.now());
        exportJobRepository.save(saved);

        // Ghi Audit Log bằng String trực tiếp để không cần sửa file AuditAction.java
        auditService.log("EXPORT_JOB_CREATED", "export_jobs", saved.getId(),
                Map.of("hackathonId", hackathonId, "type", req.getType().name()));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getById(Integer jobId) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", jobId));
        return toResponse(job);
    }

    @Override
    public String downloadUrl(Integer jobId) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", jobId));

        // Rào chắn: Chỉ trả link tải khi file đã chuẩn bị xong
        if (job.getStatus() != ExportJobStatus.DONE) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "File báo cáo chưa được xử lý xong. Trạng thái hiện tại: " + job.getStatus());
        }

        if (job.getFileUrl() == null || job.getFileUrl().isBlank()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Lỗi hệ thống: Không tìm thấy đường dẫn file trên bộ nhớ Cloud.");
        }

        // FR-36: Audit log việc download file nhạy cảm
        auditService.log("EXPORT_FILE_DOWNLOADED", "export_jobs", jobId,
                Map.of("hackathonId", job.getHackathon().getId(), "fileUrl", job.getFileUrl()));

        return job.getFileUrl();
    }

    private static ExportJobResponse toResponse(ExportJob job) {
        return ExportJobResponse.builder()
                .id(job.getId())
                .hackathonId(job.getHackathon().getId())
                .type(job.getType())
                .status(job.getStatus())
                .fileUrl(job.getFileUrl())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }
}