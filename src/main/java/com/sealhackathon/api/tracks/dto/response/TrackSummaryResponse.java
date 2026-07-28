package com.sealhackathon.api.tracks.dto.response;

import com.sealhackathon.api.tracks.value_object.TrackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TrackSummaryResponse {

    private final Integer id;
    private final Integer roundId;
    private final String name;
    private final String description;
    private final String topic;
    private final TrackStatus status;
    private final Integer sequenceOrder;
    private final Integer minTeamSize;
    private final Integer maxTeamSize;
    private final Integer maxTeams;
    private final Integer maxTeamsPerGroup;
    private final String problemStatementUrl;
    private final String problemStatementFilename;
    /** Override thời lượng thuyết trình (phút) — GĐ3; null = dùng default của round. */
    private final Integer presentationMinutes;
    /** Override thời lượng Q&A (phút) — GĐ3; null = dùng default của round. */
    private final Integer qaMinutes;
    private final java.time.LocalDateTime problemReleasedAt;
}