package com.sealhackathon.api.me.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MarkNotificationsReadRequest {

    @NotEmpty
    private List<Integer> notificationIds;
}
