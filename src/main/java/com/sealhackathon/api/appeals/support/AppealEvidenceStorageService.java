package com.sealhackathon.api.appeals.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppealEvidenceStorageService {

    public static final String KEY_PREFIX = "appeal-evidences/";

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "mov");

    private final ObjectStorageService objectStorageService;

    public String store(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn file minh chứng");
        }

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        String ext = extensionOf(file.getOriginalFilename());
        boolean image = contentType.startsWith("image/") && IMAGE_EXT.contains(ext);
        boolean video = contentType.startsWith("video/") && VIDEO_EXT.contains(ext);
        if (!image && !video) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Chỉ chấp nhận ảnh (jpg/png/webp/gif) hoặc video (mp4/webm/mov)");
        }
        long max = image ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES;
        if (file.getSize() > max) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    image ? "Ảnh minh chứng tối đa 5MB" : "Video minh chứng tối đa 50MB");
        }

        String key = KEY_PREFIX + userId + "/evidence-" + UUID.randomUUID() + "." + ext;
        try {
            objectStorageService.put(key, file.getInputStream(), contentType, file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR,
                    "Không thể lưu minh chứng, vui lòng thử lại");
        }
        return key;
    }

    public static boolean isStorageKey(String value) {
        return StringUtils.hasText(value) && value.startsWith(KEY_PREFIX);
    }

    private static String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
