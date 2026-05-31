package com.sealhackathon.api.export_jobs.dto.response;

import com.sealhackathon.api.export_jobs.value_object.ExportJobStatus;
import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobResponse {

    private Integer id;
    private Integer hackathonId;
    private ExportJobType type;
    private ExportJobStatus status;
    private String fileUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
