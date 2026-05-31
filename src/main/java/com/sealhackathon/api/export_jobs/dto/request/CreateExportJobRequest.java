package com.sealhackathon.api.export_jobs.dto.request;

import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExportJobRequest {

    @NotNull
    private ExportJobType type;
}
