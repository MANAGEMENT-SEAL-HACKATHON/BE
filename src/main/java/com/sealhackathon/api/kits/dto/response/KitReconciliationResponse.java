package com.sealhackathon.api.kits.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitReconciliationResponse {

    private List<KitReconciliationLineResponse> lines;
    private LocalDateTime kickoffStartsAt;
    private Boolean beforeKickoff;
}
