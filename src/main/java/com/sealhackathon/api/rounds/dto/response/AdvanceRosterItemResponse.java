package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một dòng danh sách CK / loại (Bug3 — advance-roster).
 * {@code status}: ADVANCED | ELIMINATED; {@code reasonCode}: TOP_N | OUT | DQ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvanceRosterItemResponse {

    private Integer teamId;
    private String teamName;
    private Integer trackId;
    private String trackName;
    private String status;
    private String reasonCode;
    private String reasonLabel;
    private Integer rank;
    private Double totalScore;
    private String assignedGroup;
}
