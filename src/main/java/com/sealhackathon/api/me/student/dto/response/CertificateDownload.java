package com.sealhackathon.api.me.student.dto.response;

import com.sealhackathon.api.storage.StoredObject;

public record CertificateDownload(StoredObject content, String filename) {}
