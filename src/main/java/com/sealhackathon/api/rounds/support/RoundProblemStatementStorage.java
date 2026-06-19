package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.storage.ObjectStorageService;
import com.sealhackathon.api.storage.StorageProperties;
import com.sealhackathon.api.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RoundProblemStatementStorage {

    public static final String KEY_PREFIX = "round-problems/";

    /** PDF tối thiểu hợp lệ — dùng cho dev seed. */
    private static final byte[] SEED_PDF_BYTES = (
            "%PDF-1.4\n"
                    + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                    + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                    + "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
                    + "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \n"
                    + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF")
            .getBytes(StandardCharsets.US_ASCII);

    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;

    public void validatePdf(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn file PDF đề bài");
            }
            return;
        }
        long maxBytes = storageProperties.getSubmissionSlideMaxMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException(
                    ErrorCode.INVALID_SLIDE_FILE,
                    "File đề bài tối đa " + storageProperties.getSubmissionSlideMaxMb() + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("pdf")) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "Đề bài phải là file PDF");
        }
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "Đề bài phải có đuôi .pdf");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(4);
            if (header.length < 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "File không phải PDF hợp lệ");
            }
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE, "Không đọc được file đề bài");
        }
    }

    public void store(Round round, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        validatePdf(file, true);
        if (StringUtils.hasText(round.getProblemStatementStorageKey())) {
            deleteQuietly(round.getProblemStatementStorageKey());
        }
        String key = buildKey(round);
        try {
            objectStorageService.put(key, file.getInputStream(), "application/pdf", file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không upload được đề bài");
        }
        round.setProblemStatementStorageKey(key);
        round.setProblemStatementOriginalFilename(file.getOriginalFilename());
        round.setProblemStatementUrl(null);
    }

    public StoredObject load(Round round) {
        if (!StringUtils.hasText(round.getProblemStatementStorageKey())) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Vòng thi chưa có file đề bài");
        }
        return objectStorageService.get(round.getProblemStatementStorageKey());
    }

    public void storeSeedPdf(Round round, String originalFilename) {
        if (hasStoredFile(round)) {
            return;
        }
        String key = buildKey(round);
        objectStorageService.put(
                key,
                new ByteArrayInputStream(SEED_PDF_BYTES),
                "application/pdf",
                SEED_PDF_BYTES.length);
        round.setProblemStatementStorageKey(key);
        round.setProblemStatementOriginalFilename(
                StringUtils.hasText(originalFilename) ? originalFilename : "de-bai-chung-ket.pdf");
        round.setProblemStatementUrl(null);
    }

    public static boolean hasStoredFile(Round round) {
        return round != null && StringUtils.hasText(round.getProblemStatementStorageKey());
    }

    public static boolean hasProblemFile(Round round) {
        return hasStoredFile(round)
                || (round != null && StringUtils.hasText(round.getProblemStatementUrl()));
    }

    public static String buildKey(Round round) {
        Integer hackathonId = round.getHackathon().getId();
        Integer roundId = round.getId() != null ? round.getId() : 0;
        return KEY_PREFIX + hackathonId + "/" + roundId + "/problem.pdf";
    }

    public static String displayFilename(Round round) {
        if (hasStoredFile(round)) {
            String name = round.getProblemStatementOriginalFilename();
            if (!StringUtils.hasText(name)) {
                return "de-bai-chung-ket.pdf";
            }
            return name.replace("\"", "").replace("\\", "");
        }
        if (round != null && StringUtils.hasText(round.getProblemStatementUrl())) {
            return "de-bai-chung-ket.pdf";
        }
        return null;
    }

    private void deleteQuietly(String storageKey) {
        try {
            objectStorageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // file cũ có thể đã mất
        }
    }
}
