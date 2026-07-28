package com.sealhackathon.api.hackathons.support;

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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HackathonBannerStorageService {

    public static final String KEY_PREFIX = "hackathon-banners/";

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ObjectStorageService objectStorageService;

    public String store(Integer hackathonId, MultipartFile file, String previousStorageKey) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh banner");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Ảnh banner tối đa 5MB");
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

        if (StringUtils.hasText(previousStorageKey) && isStorageKey(previousStorageKey)) {
            deleteQuietly(previousStorageKey);
        }

        String key = KEY_PREFIX + hackathonId + "/banner-" + UUID.randomUUID() + "." + ext;
        try {
            objectStorageService.put(key, file.getInputStream(), contentType, file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    ErrorCode.INTERNAL_ERROR,
                    "Không thể lưu ảnh banner, vui lòng thử lại");
        }
        return key;
    }

    public String storeDefaultBanner(Integer hackathonId, String title) {
        deleteQuietlyIfOwned(hackathonId, null);
        String key = KEY_PREFIX + hackathonId + "/banner-default.png";
        try {
            byte[] png = renderDefaultBanner(title);
            objectStorageService.put(key, new ByteArrayInputStream(png), "image/png", png.length);
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    ErrorCode.INTERNAL_ERROR,
                    "Không thể tạo ảnh banner mặc định");
        }
        return key;
    }

    public Resource loadAsResource(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa có ảnh banner");
        }
        if (!isStorageKey(storageKey)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Banner không hợp lệ");
        }
        StoredObject stored = objectStorageService.get(storageKey);
        String filename = storageKey.substring(storageKey.lastIndexOf('/') + 1);
        return StoredObjectResource.toResource(stored, filename);
    }

    public static boolean isStorageKey(String value) {
        return StringUtils.hasText(value) && value.startsWith(KEY_PREFIX);
    }

    public void deleteQuietlyIfOwned(Integer hackathonId, String storageKey) {
        if (!StringUtils.hasText(storageKey) || !isStorageKey(storageKey)) {
            return;
        }
        String expectedPrefix = KEY_PREFIX + hackathonId + "/";
        if (!storageKey.startsWith(expectedPrefix)) {
            return;
        }
        deleteQuietly(storageKey);
    }

    private void deleteQuietly(String storageKey) {
        try {
            objectStorageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // file cũ có thể đã mất
        }
    }

    private static byte[] renderDefaultBanner(String title) throws IOException {
        int width = 1200;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0, 114, 255));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(255, 255, 255, 40));
        graphics.fillOval(width - 280, -80, 360, 360);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 42));
        String label = StringUtils.hasText(title) ? title : "SEAL Hackathon";
        graphics.drawString(label, 48, height / 2 + 14);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
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
