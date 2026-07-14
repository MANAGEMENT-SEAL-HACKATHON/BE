package com.sealhackathon.api.tracks.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.support.SeedProblemPdf;
import com.sealhackathon.api.storage.ObjectStorageService;
import com.sealhackathon.api.storage.StoredObject;
import com.sealhackathon.api.tracks.entity.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TrackProblemStatementStorage {

    public static final String KEY_PREFIX = "track-problems/";

    private final ObjectStorageService objectStorageService;
    private final RoundProblemStatementStorage roundProblemStatementStorage;

    public void store(Track track, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        roundProblemStatementStorage.validatePdf(file, true);
        if (StringUtils.hasText(track.getProblemStatementStorageKey())) {
            deleteQuietly(track.getProblemStatementStorageKey());
        }
        String key = buildKey(track);
        try {
            objectStorageService.put(key, file.getInputStream(), "application/pdf", file.getSize());
        } catch (IOException ex) {
            throw new BusinessRuleException(ErrorCode.INTERNAL_ERROR, "Không upload được đề bài");
        }
        track.setProblemStatementStorageKey(key);
        track.setProblemStatementOriginalFilename(file.getOriginalFilename());
        track.setProblemStatementUrl(null);
    }

    public StoredObject load(Track track) {
        if (!StringUtils.hasText(track.getProblemStatementStorageKey())) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "Bảng đấu chưa có file đề bài");
        }
        return objectStorageService.get(track.getProblemStatementStorageKey());
    }

    public void storeSeedPdf(Track track, String originalFilename) {
        if (hasStoredFile(track)) {
            return;
        }
        String key = buildKey(track);
        byte[] pdf = SeedProblemPdf.bytes();
        objectStorageService.put(
                key,
                new ByteArrayInputStream(pdf),
                "application/pdf",
                pdf.length);
        track.setProblemStatementStorageKey(key);
        String filename = StringUtils.hasText(originalFilename)
                ? originalFilename
                : SeedProblemPdf.displayFilename();
        track.setProblemStatementOriginalFilename(filename);
        track.setProblemStatementUrl(null);
    }

    public static boolean hasStoredFile(Track track) {
        return track != null && StringUtils.hasText(track.getProblemStatementStorageKey());
    }

    public static boolean hasProblemFile(Track track) {
        return hasStoredFile(track)
                || (track != null && StringUtils.hasText(track.getProblemStatementUrl()));
    }

    public static String buildKey(Track track) {
        Integer hackathonId = track.getRound().getHackathon().getId();
        Integer roundId = track.getRound().getId();
        Integer trackId = track.getId() != null ? track.getId() : 0;
        return KEY_PREFIX + hackathonId + "/" + roundId + "/" + trackId + "/problem.pdf";
    }

    public static String displayFilename(Track track) {
        if (hasStoredFile(track)) {
            String name = track.getProblemStatementOriginalFilename();
            if (!StringUtils.hasText(name)) {
                return "de-bai-track.pdf";
            }
            return name.replace("\"", "").replace("\\", "");
        }
        if (track != null && StringUtils.hasText(track.getProblemStatementUrl())) {
            return "de-bai-track.pdf";
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
