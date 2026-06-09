package com.sealhackathon.api.storage;

import org.springframework.core.io.InputStreamResource;

public final class StoredObjectResource {

    private StoredObjectResource() {
    }

    public static InputStreamResource toResource(StoredObject stored, String filename) {
        return new InputStreamResource(stored.stream()) {
            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public long contentLength() {
                return stored.sizeBytes();
            }
        };
    }
}
