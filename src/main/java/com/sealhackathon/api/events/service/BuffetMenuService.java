package com.sealhackathon.api.events.service;

import com.sealhackathon.api.events.dto.request.BuffetMenuItemRequest;
import com.sealhackathon.api.events.dto.response.BuffetMenuItemResponse;

import java.util.List;

public interface BuffetMenuService {

    List<BuffetMenuItemResponse> listByEvent(Integer eventId);

    List<BuffetMenuItemResponse> replaceMenu(Integer eventId, List<BuffetMenuItemRequest> items);
}
