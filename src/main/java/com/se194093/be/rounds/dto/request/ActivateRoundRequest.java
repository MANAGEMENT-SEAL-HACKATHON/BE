package com.se194093.be.rounds.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-06B PATCH /api/v1/rounds/{id}/activate. Body optional — note dùng cho audit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivateRoundRequest {

    @Size(max = 1000)
    private String note;
}
