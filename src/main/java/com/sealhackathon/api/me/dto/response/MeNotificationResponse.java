package com.sealhackathon.api.me.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeNotificationResponse {

    private Integer id;
    private String type;
    private String title;
    private String body;
    private Boolean isRead;
    private LocalDateTime sentAt;
}
