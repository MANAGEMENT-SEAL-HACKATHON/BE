package com.sealhackathon.api.live_scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementEvent {
    public static final String TYPE = "ANNOUNCEMENT";
    public static final String RESULTS_PUBLISHED = "RESULTS_PUBLISHED";

    private String type;
    private String kind;
    private Integer hackathonId;
    private Integer roundId;
    private String title;
    private String message;
    private String timestamp;
}
