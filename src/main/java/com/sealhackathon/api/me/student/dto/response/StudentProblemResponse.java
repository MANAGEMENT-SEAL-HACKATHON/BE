package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProblemResponse {

    private Integer roundId;
    private String problemStatement;
    private String problemUrl;
    private String problemDownloadPath;
    private String problemFilename;
    private Boolean released;
    /**
     * False when finalist cannot resolve prelim-track PDF (empty-state, not HTTP 500).
     * Null/true for normal available problems.
     */
    private Boolean available;
    /** Prelim track name — set when final reuses track PDF. */
    private Integer trackId;
    private String trackName;
}
