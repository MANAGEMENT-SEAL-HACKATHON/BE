package com.sealhackathon.api.users.service;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.storage.ObjectStorageService;
import com.sealhackathon.api.storage.StoredObject;
import com.sealhackathon.api.storage.StoredObjectResource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentCardStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final String KEY_PREFIX = "student-cards/";

    private final ObjectStorageService objectStorageService;

    public String store(Integer userId, MultipartFile file, String previousStorageKey) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh thẻ sinh viên");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Ảnh thẻ tối đa 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Chỉ chấp nhận file ảnh");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessRuleException(
                    ErrorCode.VALIDATION_FAILED,
                    "Định dạng ảnh không hợp lệ (chỉ hỗ trợ jpg, jpeg, png, webp)");
        }

        if (StringUtils.hasText(previousStorageKey)) {
            deleteQuietly(previousStorageKey);
        }

        String fileName = "student-card-" + userId + "-" + UUID.randomUUID() + "." + ext;
        String key = KEY_PREFIX + userId + "/" + fileName;
        try {
            objectStorageService.put(key, file.getInputStream(), contentType, file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    ErrorCode.INTERNAL_ERROR,
                    "Không thể lưu ảnh thẻ sinh viên, vui lòng thử lại");
        }
        return key;
    }

    public Resource loadAsResource(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa có ảnh thẻ sinh viên");
        }
        String key = normalizeKey(storageKey);
        StoredObject stored = objectStorageService.get(key);
        String filename = key.substring(key.lastIndexOf('/') + 1);
        return StoredObjectResource.toResource(stored, filename);
    }

    static String normalizeKey(String storageKey) {
        if (storageKey.startsWith(KEY_PREFIX)) {
            return storageKey;
        }
        // Legacy path từ local disk: "{userId}/student-card-....jpg"
        if (storageKey.matches("\\d+/student-card-.*")) {
            return KEY_PREFIX + storageKey;
        }
        return storageKey;
    }

    private void deleteQuietly(String storageKey) {
        try {
            objectStorageService.delete(normalizeKey(storageKey));
        } catch (RuntimeException ignored) {
            // file cũ có thể đã mất — không chặn upload mới
        }
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
