package com.sealhackathon.api.hackathons.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-06 PATCH /api/v1/hackathons/{id}/status
 *
 * <p>State machine một chiều: DRAFT &rarr; ONGOING &rarr; PENDING_CONFIRM &rarr; FINISHED.
 * Gate cứng validate tổng weight Criteria mọi Round = 1.0 khi DRAFT → ONGOING.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeHackathonStatusRequest {

    /** §10.3: {@code "status": "ONGOING"} — {@link JsonAlias} giữ tương thích {@code targetStatus}. */
    @NotNull
    @JsonAlias("status")
    private HackathonStatus targetStatus;

    @Size(max = 1000)
    private String note;
}
