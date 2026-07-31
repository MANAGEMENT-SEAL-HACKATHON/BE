package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.events.dto.request.BuffetMenuItemRequest;
import com.sealhackathon.api.events.dto.response.BuffetMenuItemResponse;
import com.sealhackathon.api.events.entity.BuffetMenuItem;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.mapper.BuffetMenuItemMapper;
import com.sealhackathon.api.events.repository.BuffetMenuItemRepository;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.BuffetMenuService;
import com.sealhackathon.api.events.support.BuffetEditGuard;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BuffetMenuServiceImpl implements BuffetMenuService {

    private final EventRepository eventRepository;
    private final BuffetMenuItemRepository buffetMenuItemRepository;
    private final BuffetMenuItemMapper buffetMenuItemMapper;
    private final BuffetEditGuard buffetEditGuard;
    private final HackathonArchiveGuard archiveGuard;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    @Transactional(readOnly = true)
    public List<BuffetMenuItemResponse> listByEvent(Integer eventId) {
        Event event = requireBuffetEvent(eventId);
        assertCanView(event);
        return buffetMenuItemRepository.findByEvent_IdOrderByDisplayOrderAscIdAsc(eventId).stream()
                .map(buffetMenuItemMapper::toResponse)
                .toList();
    }

    @Override
    public List<BuffetMenuItemResponse> replaceMenu(Integer eventId, List<BuffetMenuItemRequest> items) {
        Event event = requireBuffetEvent(eventId);
        archiveGuard.assertNotArchived(event.getHackathon());
        buffetEditGuard.assertEditable(event.getHackathon().getId());

        buffetMenuItemRepository.deleteByEvent_Id(eventId);
        buffetMenuItemRepository.flush();

        List<BuffetMenuItemRequest> body = items == null ? List.of() : items;
        List<BuffetMenuItem> saved = new ArrayList<>();
        int order = 0;
        for (BuffetMenuItemRequest req : body) {
            saved.add(buffetMenuItemRepository.save(
                    buffetMenuItemMapper.toEntity(req, event, order++)));
        }
        return saved.stream().map(buffetMenuItemMapper::toResponse).toList();
    }

    private Event requireBuffetEvent(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        if (event.getType() != EventType.BUFFET) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Thực đơn buffet chỉ áp dụng cho sự kiện type=BUFFET",
                    Map.of("eventId", eventId, "type", event.getType().name()));
        }
        return event;
    }

    private void assertCanView(Event event) {
        CurrentUserStub user = currentUserAccessor.currentUser();
        if (user != null && user.getRole() == UserRole.COORDINATOR) {
            return;
        }
        if (!Boolean.TRUE.equals(event.getIsPublic())) {
            throw new AccessDeniedException("Sự kiện buffet không công khai");
        }
    }
}
