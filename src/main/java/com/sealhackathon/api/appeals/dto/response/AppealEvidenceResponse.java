package com.sealhackathon.api.appeals.dto.response;

import com.sealhackathon.api.appeals.value_object.AppealEvidenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealEvidenceResponse {

    private Integer id;
    private String url;
    private AppealEvidenceType type;
    private String caption;
    private Integer displayOrder;
}
