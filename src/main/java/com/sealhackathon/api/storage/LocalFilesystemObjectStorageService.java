package com.sealhackathon.api.storage;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
/** Chỉ dùng trong test (`app.storage.type=local`). Runtime dev/prod dùng MinIO. */
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalFilesystemObjectStorageService implements ObjectStorageService {

    private final Path rootDir;

    public LocalFilesystemObjectStorageService(StorageProperties properties) {
        this.rootDir = Path.of(properties.getLocalDir()).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream stream, String contentType, long sizeBytes) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không thể lưu file");
        }
    }

    @Override
    public StoredObject get(String key) {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy file");
        }
        try {
            String contentType = Files.probeContentType(target);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return new StoredObject(Files.newInputStream(target), contentType, Files.size(target));
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không đọc được file");
        }
    }

    @Override
    public void delete(String key) {
        Path target = resolve(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không xóa được file");
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    private Path resolve(String key) {
        Path resolved = rootDir.resolve(key).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Đường dẫn file không hợp lệ");
        }
        return resolved;
    }
}
