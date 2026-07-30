package com.sealhackathon.api.appeals.dto.request;

import com.sealhackathon.api.appeals.value_object.AppealEvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AppealEvidenceItemRequest {

    @NotBlank
    private String url;

    @NotNull
    private AppealEvidenceType type;

    private String caption;

    private Integer displayOrder;
}
