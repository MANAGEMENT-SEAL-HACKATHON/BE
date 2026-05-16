package com.se194093.be.hackathons.dto.request;

import com.se194093.be.hackathons.value_object.HackathonStatus;
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

    @NotNull
    private HackathonStatus targetStatus;

    @Size(max = 1000)
    private String note;
}
