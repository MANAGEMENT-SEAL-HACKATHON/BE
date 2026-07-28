package com.sealhackathon.api.export_jobs.dto.response;

import com.sealhackathon.api.storage.StoredObject;

import java.io.IOException;

public record ExportFileDownload(StoredObject content, String filename) {

    public byte[] readBytes() throws IOException {
        try (var stream = content.stream()) {
            return stream.readAllBytes();
        }
    }
}
