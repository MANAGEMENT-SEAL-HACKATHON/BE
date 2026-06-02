package com.sealhackathon.api.me.student.dto.response;

import com.sealhackathon.api.appeals.value_object.AppealStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealResponse {

    private Integer id;
    private Integer teamId;
    private Integer roundId;
    private String reason;
    private String evidenceUrl;
    private AppealStatus status;
}
