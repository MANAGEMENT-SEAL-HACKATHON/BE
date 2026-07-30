package com.sealhackathon.api.showcase.support;

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
public class ShowcaseCoverStorageService {

    public static final String KEY_PREFIX = "showcase/";

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ObjectStorageService objectStorageService;

    public String storeCover(Integer articleId, MultipartFile file, String previousKey) {
        return store(articleId, "cover", file, previousKey);
    }

    public String storeBlockImage(Integer articleId, MultipartFile file) {
        return store(articleId, "block", file, null);
    }

    public Resource loadAsResource(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !isStorageKey(storageKey)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Ảnh không hợp lệ");
        }
        StoredObject stored = objectStorageService.get(storageKey);
        String filename = storageKey.substring(storageKey.lastIndexOf('/') + 1);
        return StoredObjectResource.toResource(stored, filename);
    }

    public static boolean isStorageKey(String value) {
        return StringUtils.hasText(value) && value.startsWith(KEY_PREFIX);
    }

    public void deleteQuietly(String storageKey) {
        if (!isStorageKey(storageKey)) {
            return;
        }
        try {
            objectStorageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // ignore missing files
        }
    }

    private String store(Integer articleId, String kind, MultipartFile file, String previousKey) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Ảnh tối đa 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Chỉ chấp nhận file ảnh");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessRuleException(
                    ErrorCode.VALIDATION_FAILED,
                    "Định dạng ảnh không hợp lệ (jpg, jpeg, png, webp)");
        }
        if (StringUtils.hasText(previousKey) && isStorageKey(previousKey)) {
            deleteQuietly(previousKey);
        }
        String key = KEY_PREFIX + articleId + "/" + kind + "-" + UUID.randomUUID() + "." + ext;
        try {
            objectStorageService.put(key, file.getInputStream(), contentType, file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không thể lưu ảnh, vui lòng thử lại");
        }
        return key;
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
