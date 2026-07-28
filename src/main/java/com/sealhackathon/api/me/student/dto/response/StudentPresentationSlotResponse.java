package com.sealhackathon.api.me.student.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Student view of their team's presentation queue slot for a round.
 * Never includes other teams' names — only anonymous display codes.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentPresentationSlotResponse {

    /**
     * False when shuffle has not run yet ("Chưa quay số"). Prefer 200 + this flag over 404.
     */
    private Boolean available;

    private String message;

    /** sequence_order after shuffle (STT). */
    private Integer order;

    /** Anonymous code: "#" + submissionId. */
    private String displayCode;

    /** WAITING | PRESENTING | DONE | SKIPPED | ELIMINATED */
    private String status;

    private Integer trackId;

    private Boolean roundIsFinal;

    /** sequence_order of the slot currently PRESENTING (null if none). */
    private Integer currentPresentingOrder;

    /** displayCode of the currently PRESENTING slot. */
    private String currentPresentingDisplayCode;

    /**
     * Count of WAITING slots with sequenceOrder &lt; my order
     * (excludes SKIPPED, DONE, ELIMINATED, PRESENTING).
     */
    private Integer teamsAhead;

    /** Timer phase when this team's slot is live (PRESENTING / QA / PAUSED / …). */
    private String timerPhase;

    /** Remaining seconds for the live timer phase (null when not presenting). */
    private Integer remainingSeconds;
}
