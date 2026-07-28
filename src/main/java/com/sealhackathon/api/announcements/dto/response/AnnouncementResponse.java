package com.sealhackathon.api.announcements.dto.response;

import com.sealhackathon.api.announcements.entity.HackathonAnnouncement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AnnouncementResponse {

    private Integer id;
    private Integer hackathonId;
    private Integer roundId;
    private String kind;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private Boolean softHidden;

    public static AnnouncementResponse from(HackathonAnnouncement entity) {
        if (entity == null) {
            return null;
        }
        return AnnouncementResponse.builder()
                .id(entity.getId())
                .hackathonId(entity.getHackathonId())
                .roundId(entity.getRoundId())
                .kind(entity.getKind())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .softHidden(entity.getSoftHidden())
                .build();
    }
}
