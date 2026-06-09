package com.sealhackathon.api.submissions.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.storage.ObjectStorageService;
import com.sealhackathon.api.storage.StorageProperties;
import com.sealhackathon.api.storage.StoredObject;
import com.sealhackathon.api.submissions.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SubmissionSlideStorage {

    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;

    public void validatePdf(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new BusinessRuleException(ErrorCode.SLIDE_FILE_REQUIRED, "slideFile bắt buộc");
            }
            return;
        }
        long maxBytes = storageProperties.getSubmissionSlideMaxMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE,
                    "slideFile tối đa " + storageProperties.getSubmissionSlideMaxMb() + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("pdf")) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "slideFile phải là PDF");
        }
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "slideFile phải có đuôi .pdf");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(4);
            if (header.length < 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "slideFile không phải PDF hợp lệ");
            }
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "Không đọc được slideFile");
        }
    }

    public void storeSlide(Submission submission, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        validatePdf(file, true);
        if (StringUtils.hasText(submission.getSlideStorageKey())) {
            objectStorageService.delete(submission.getSlideStorageKey());
        }
        String key = buildKey(submission);
        try {
            objectStorageService.put(key, file.getInputStream(), "application/pdf", file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không upload được slide");
        }
        submission.setSlideStorageKey(key);
        submission.setSlideOriginalFilename(file.getOriginalFilename());
        submission.setSlideContentType("application/pdf");
        submission.setSlideSizeBytes(file.getSize());
        submission.setSlideUploadedAt(LocalDateTime.now());
        submission.setSlideUrl(null);
    }

    public StoredObject loadSlide(Submission submission) {
        if (!StringUtils.hasText(submission.getSlideStorageKey())) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Bài nộp chưa có slide PDF");
        }
        return objectStorageService.get(submission.getSlideStorageKey());
    }

    public static String buildKey(Submission submission) {
        Integer hackathonId = submission.getHackathon().getId();
        Integer roundId = submission.getRound().getId();
        Integer submissionId = submission.getId() != null ? submission.getId() : 0;
        return "submissions/%d/%d/%d/slide.pdf".formatted(hackathonId, roundId, submissionId);
    }

    /** Tên file gốc khi upload — hiển thị cho student/judge xác nhận đúng bản PDF. */
    public static String displayFilename(Submission submission) {
        if (submission == null || !StringUtils.hasText(submission.getSlideStorageKey())) {
            return null;
        }
        String name = submission.getSlideOriginalFilename();
        if (!StringUtils.hasText(name)) {
            return "slide.pdf";
        }
        return name.replace("\"", "").replace("\\", "");
    }
}
