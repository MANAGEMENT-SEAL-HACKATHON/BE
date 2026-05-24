package com.sealhackathon.api.events.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.hackathons.entity.Hackathon;

import java.util.List;

/**
 * Validator 3 lớp cho lịch sự kiện (FR-06, MF-01 v3.1).
 *
 * <ul>
 *   <li><b>Lớp 1</b> — block 422 {@code EVENT_OUT_OF_HACKATHON}</li>
 *   <li><b>Lớp 2</b> — block 422 {@code EVENT_OVERLAP} (KICKOFF/AWARDS)</li>
 *   <li><b>Lớp 3</b> — block 422 {@code EVENT_ORDER_VIOLATION} / {@code EVENT_END_REQUIRED}:
 *       WORKSHOP → KICKOFF → AWARDS; WORKSHOP và KICKOFF phải ở hai ngày khác nhau</li>
 *   <li><b>FIX-R10</b> — block 422 {@code EVENT_LOCATION_REQUIRED}: ít nhất location hoặc meetUrl</li>
 * </ul>
 */
public interface EventScheduleValidator {

    void validateBlocking(Hackathon hackathon, CreateEventRequest req, Integer excludeEventId);

    void validateBlocking(Hackathon hackathon, UpdateEventRequest req, Integer excludeEventId);

    /**
     * Chỉ Lớp 3d — warn mềm, không block.
     */
    List<Warning> computeLayer3Warnings(Hackathon hackathon, CreateEventRequest req);

    List<Warning> computeLayer3Warnings(Hackathon hackathon, UpdateEventRequest req);
}
