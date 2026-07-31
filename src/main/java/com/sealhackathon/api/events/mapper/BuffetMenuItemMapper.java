package com.sealhackathon.api.events.mapper;

import com.sealhackathon.api.events.dto.request.BuffetMenuItemRequest;
import com.sealhackathon.api.events.dto.response.BuffetMenuItemResponse;
import com.sealhackathon.api.events.entity.BuffetMenuItem;
import com.sealhackathon.api.events.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class BuffetMenuItemMapper {

    public BuffetMenuItem toEntity(BuffetMenuItemRequest req, Event event, int fallbackOrder) {
        return BuffetMenuItem.builder()
                .event(event)
                .name(req.getName())
                .quantity(req.getQuantity())
                .unit(req.getUnit())
                .note(req.getNote())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : fallbackOrder)
                .build();
    }

    public BuffetMenuItemResponse toResponse(BuffetMenuItem item) {
        if (item == null) {
            return null;
        }
        return BuffetMenuItemResponse.builder()
                .id(item.getId())
                .eventId(item.getEvent() == null ? null : item.getEvent().getId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .note(item.getNote())
                .displayOrder(item.getDisplayOrder())
                .build();
    }
}
