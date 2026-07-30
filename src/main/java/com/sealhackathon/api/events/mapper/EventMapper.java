package com.sealhackathon.api.events.mapper;

import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.events.dto.response.EventResponse;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public Event toEntity(CreateEventRequest req, Hackathon hackathon) {
        return Event.builder()
                .hackathon(hackathon)
                .title(req.getTitle())
                .type(req.getType())
                .description(req.getDescription())
                .location(req.getLocation())
                .meetUrl(req.getMeetUrl())
                .buffetLocation(req.getBuffetLocation())
                .buffetStartsAt(req.getBuffetStartsAt())
                .buffetEndsAt(req.getBuffetEndsAt())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .isPublic(req.getIsPublic() == null ? Boolean.TRUE : req.getIsPublic())
                .build();
    }

    public void applyUpdate(Event entity, UpdateEventRequest req) {
        entity.setTitle(req.getTitle());
        entity.setType(req.getType());
        entity.setDescription(req.getDescription());
        entity.setLocation(req.getLocation());
        entity.setMeetUrl(req.getMeetUrl());
        entity.setBuffetLocation(req.getBuffetLocation());
        entity.setBuffetStartsAt(req.getBuffetStartsAt());
        entity.setBuffetEndsAt(req.getBuffetEndsAt());
        entity.setStartsAt(req.getStartsAt());
        entity.setEndsAt(req.getEndsAt());
        if (req.getIsPublic() != null) {
            entity.setIsPublic(req.getIsPublic());
        }
    }

    public EventResponse toResponse(Event e) {
        if (e == null) {
            return null;
        }
        return EventResponse.builder()
                .id(e.getId())
                .hackathonId(e.getHackathon() == null ? null : e.getHackathon().getId())
                .title(e.getTitle())
                .type(e.getType())
                .description(e.getDescription())
                .location(e.getLocation())
                .meetUrl(e.getMeetUrl())
                .buffetLocation(e.getBuffetLocation())
                .buffetStartsAt(e.getBuffetStartsAt())
                .buffetEndsAt(e.getBuffetEndsAt())
                .startsAt(e.getStartsAt())
                .endsAt(e.getEndsAt())
                .isPublic(e.getIsPublic())
                .createdById(e.getCreatedBy() == null ? null : e.getCreatedBy().getId())
                .build();
    }
}
