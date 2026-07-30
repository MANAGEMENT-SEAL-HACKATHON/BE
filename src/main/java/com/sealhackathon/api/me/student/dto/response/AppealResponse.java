package com.sealhackathon.api.me.student.dto.response;

import com.sealhackathon.api.appeals.dto.response.AppealEvidenceResponse;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealResponse {

    private Integer id;
    private Integer teamId;
    private String teamName;
    private Integer roundId;
    private String roundName;
    private String reason;
    private String evidenceUrl;
    private List<AppealEvidenceResponse> evidences;
    private AppealStatus status;
    private String decisionNote;
    private Integer reviewedById;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
