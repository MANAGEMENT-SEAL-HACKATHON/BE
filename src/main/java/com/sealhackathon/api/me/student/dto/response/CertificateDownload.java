package com.sealhackathon.api.me.student.dto.response;

import com.sealhackathon.api.storage.StoredObject;

import java.io.IOException;

public record CertificateDownload(StoredObject content, String filename) {

    public byte[] readBytes() throws IOException {
        try (var stream = content.stream()) {
            return stream.readAllBytes();
        }
    }
}
