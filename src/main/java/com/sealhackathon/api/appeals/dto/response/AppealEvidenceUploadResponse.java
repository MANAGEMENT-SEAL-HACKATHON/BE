package com.sealhackathon.api.appeals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealEvidenceUploadResponse {

    private String storageKey;
    private String contentType;
    private long sizeBytes;
}
