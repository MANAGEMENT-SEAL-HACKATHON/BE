package com.sealhackathon.api.me.dto.response;

import com.sealhackathon.api.common.value_object.AssignmentResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponseStatusResponse {

    private Integer assignmentId;
    private String assignmentKind;
    private AssignmentResponseStatus responseStatus;
    private LocalDateTime respondedAt;
    private String declineReason;
}
