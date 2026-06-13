package com.sealhackathon.api.export_jobs.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.export_jobs.dto.request.CreateExportJobRequest;
import com.sealhackathon.api.export_jobs.dto.response.ExportFileDownload;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;
import com.sealhackathon.api.export_jobs.entity.ExportJob;
import com.sealhackathon.api.export_jobs.repository.ExportJobRepository;
import com.sealhackathon.api.export_jobs.service.ExportJobService;
import com.sealhackathon.api.export_jobs.support.ExportCsvBuilder;
import com.sealhackathon.api.export_jobs.value_object.ExportJobStatus;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.storage.ObjectStorageService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Map;

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
    private final ExportCsvBuilder exportCsvBuilder;
    private final ObjectStorageService objectStorage;

    @Override
    public ExportJobResponse create(Integer hackathonId, CreateExportJobRequest req) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() != HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ có thể xuất báo cáo khi Hackathon đã chính thức đóng sổ (FINISHED).");
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
        byte[] csvBytes = exportCsvBuilder.build(hackathon, req.getType());
        String storageKey = "exports/" + hackathonId + "/" + saved.getId() + "/" + req.getType().name() + ".csv";
        objectStorage.put(storageKey, new ByteArrayInputStream(csvBytes), "text/csv", csvBytes.length);

        saved.setStatus(ExportJobStatus.DONE);
        saved.setFileUrl(storageKey);
        saved.setFinishedAt(LocalDateTime.now());
        exportJobRepository.save(saved);

        log.info("[ExportJob] Completed job {} type {} ({} bytes)", saved.getId(), req.getType(), csvBytes.length);
        auditService.log(AuditAction.EXPORT_JOB_CREATED, "export_jobs", saved.getId(),
                Map.of("hackathonId", hackathonId, "type", req.getType().name(), "storageKey", storageKey));

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
    @Transactional(readOnly = true)
    public ExportFileDownload downloadFile(Integer jobId) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", jobId));

        if (job.getStatus() != ExportJobStatus.DONE) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "File báo cáo chưa được xử lý xong. Trạng thái hiện tại: " + job.getStatus());
        }
        if (job.getFileUrl() == null || job.getFileUrl().isBlank()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Không tìm thấy file export.");
        }
        if (!objectStorage.exists(job.getFileUrl())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "File export không còn trên storage.");
        }

        auditService.log(AuditAction.EXPORT_FILE_DOWNLOADED, "export_jobs", jobId,
                Map.of("hackathonId", job.getHackathon().getId(), "storageKey", job.getFileUrl()));

        String filename = "hackathon-" + job.getHackathon().getId() + "-" + job.getType().name() + ".csv";
        return new ExportFileDownload(objectStorage.get(job.getFileUrl()), filename);
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
