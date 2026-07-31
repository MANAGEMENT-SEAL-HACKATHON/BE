package com.sealhackathon.api.events.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.events.value_object.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse {

    private final Integer id;
    private final Integer hackathonId;
    private final String title;
    private final EventType type;
    private final String description;
    private final String location;
    private final String meetUrl;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Boolean isPublic;
    private final Integer createdById;
}
