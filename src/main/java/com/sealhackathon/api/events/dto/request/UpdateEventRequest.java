package com.sealhackathon.api.events.dto.request;

import com.sealhackathon.api.events.value_object.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {

    @NotBlank
    @Size(max = 300)
    private String title;

    @NotNull
    private EventType type;

    private String description;

    @Size(max = 300)
    private String location;

    private String meetUrl;

    @NotNull
    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private Boolean isPublic;
}
