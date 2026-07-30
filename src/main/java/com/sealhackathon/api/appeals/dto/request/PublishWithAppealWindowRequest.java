package com.sealhackathon.api.appeals.dto.request;

import com.sealhackathon.api.appeals.value_object.AppealWindowMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishWithAppealWindowRequest {

    private AppealWindowMode appealWindowMode;

    /** Optional override for DELAY_FINAL shortfall; default = configured − remaining. */
    private Integer delayMinutes;

    /** Required when mode = SKIP. */
    private String skipReason;
}
