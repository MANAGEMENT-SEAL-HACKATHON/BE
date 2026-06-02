package com.sealhackathon.api.me.student.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelotteryTrackRequest {

    @NotNull
    private Integer trackId;
}
