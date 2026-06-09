package com.sealhackathon.api.storage;

import java.io.InputStream;

public interface ObjectStorageService {

    void put(String key, InputStream stream, String contentType, long sizeBytes);

    StoredObject get(String key);

    void delete(String key);

    boolean exists(String key);
}
