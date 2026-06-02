package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorPresentationSlotResponse {

    private Integer teamId;
    private LocalDateTime slotStartAt;
    private LocalDateTime slotEndAt;
    private String location;
}
