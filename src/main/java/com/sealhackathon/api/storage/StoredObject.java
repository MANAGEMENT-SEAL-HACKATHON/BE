package com.sealhackathon.api.storage;

import java.io.InputStream;

public record StoredObject(InputStream stream, String contentType, long sizeBytes) {}
