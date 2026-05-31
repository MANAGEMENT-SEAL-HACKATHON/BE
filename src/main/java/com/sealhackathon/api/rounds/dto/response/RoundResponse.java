package com.sealhackathon.api.rounds.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundResponse {

    private final Integer id;
    private final Integer hackathonId;
    /** @deprecated v3 — Round không còn FK Track; luôn null */
    @Deprecated
    private final Integer trackId;
    private final String name;
    private final LocalDateTime examAt;
    private final Boolean isFinal;
    private final RoundType roundType;
    private final LateSubmissionPolicy lateSubmissionPolicy;
    private final LocalDateTime submissionOpen;
    private final LocalDateTime submissionDeadline;
    private final Integer codingDurationHours;
    private final String problemStatementUrl;
    private final LocalDateTime problemReleasedAt;
    private final Integer topNAdvance;
    private final Boolean wildcardEnabled;
    private final Integer minTeamsFinal;
    private final TiebreakRule tiebreakRule;
    private final Boolean isActive;
    private final LocalDateTime activatedAt;
    private final Boolean scoringLocked;
    private final LocalDateTime scoringLockedAt;
    private final Integer scoringLockedById;
    private final Boolean forceLocked;
    private final String forceLockReason;
    private final Boolean isPublished;
    private final LocalDateTime publishedAt;
    private final Integer publishedById;
    private final LocalDateTime createdAt;
}
