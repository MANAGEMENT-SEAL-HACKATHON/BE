package com.sealhackathon.api.users.service;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentCardStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path rootDir;

    public StudentCardStorageService(
            @Value("${app.storage.student-card-dir:uploads/student-cards}") String studentCardDir) {
        this.rootDir = Path.of(studentCardDir).toAbsolutePath().normalize();
    }

    public String store(Integer userId, MultipartFile file) {
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

        String fileName = "student-card-" + userId + "-" + UUID.randomUUID() + "." + ext;
        Path userDir = rootDir.resolve(String.valueOf(userId)).normalize();
        Path target = userDir.resolve(fileName).normalize();
        ensureSafePath(target);
        try {
            Files.createDirectories(userDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    ErrorCode.INTERNAL_ERROR,
                    "Không thể lưu ảnh thẻ sinh viên, vui lòng thử lại");
        }
        return rootDir.relativize(target).toString().replace('\\', '/');
    }

    public Resource loadAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa có ảnh thẻ sinh viên");
        }
        Path resolved = rootDir.resolve(relativePath).normalize();
        ensureSafePath(resolved);
        if (!Files.exists(resolved)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ảnh thẻ sinh viên");
        }
        try {
            return new UrlResource(resolved.toUri());
        } catch (MalformedURLException ex) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Không đọc được ảnh thẻ sinh viên");
        }
    }

    private void ensureSafePath(Path path) {
        if (!path.startsWith(rootDir)) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Đường dẫn file không hợp lệ");
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
