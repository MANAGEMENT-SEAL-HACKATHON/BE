package com.se194093.be.events.service.impl;

import com.se194093.be.common.response.Warning;
import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.dto.request.UpdateEventRequest;
import com.se194093.be.events.service.EventScheduleValidator;
import com.se194093.be.hackathons.entity.Hackathon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-06a-events.md}.
 *
 * <p>Inject: EventRepository (overlap query).
 *
 * <p>Lưu ý:
 * <ul>
 *   <li>Buffer Lớp 1 = 1 ngày (cộng vào event_end khi check).</li>
 *   <li>Tolerance cho overlap = 0 (chính xác giờ; nếu cần làm round to phút thì document riêng).</li>
 *   <li>Lớp 2 cho phép PRESENTATION parallel khác location/meetUrl —
 *       chỉ block khi cùng type KICKOFF/AWARDS.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventScheduleValidatorImpl implements EventScheduleValidator {

    @Override
    public void validateBlocking(Hackathon hackathon, CreateEventRequest req, Integer excludeEventId) {
        // TODO Dev:
        //  - Lớp 1:
        //      if hackathon.eventStart != null && req.startsAt.toLocalDate().isBefore(hackathon.eventStart)
        //         → throw BusinessRuleException(EVENT_OUT_OF_HACKATHON, ..., {eventStart, eventEnd})
        //      effectiveEnd = hackathon.eventEnd.plusDays(1)
        //      end = req.endsAt != null ? req.endsAt : req.startsAt
        //      if end.toLocalDate().isAfter(effectiveEnd) → 422
        //  - Lớp 2:
        //      if req.type IN (KICKOFF, AWARDS):
        //          overlaps = eventRepo.findOverlapping(hackathon.id, req.type, req.startsAt,
        //                                              req.endsAt != null ? req.endsAt : req.startsAt,
        //                                              excludeEventId)
        //          if !overlaps.isEmpty() → throw 422 EVENT_OVERLAP { conflictIds }
        //  - endsAt < startsAt → throw 422 EVENT_END_BEFORE_START
        throw new UnsupportedOperationException("FR-06A validateBlocking(Create) - to be implemented");
    }

    @Override
    public void validateBlocking(Hackathon hackathon, UpdateEventRequest req, Integer excludeEventId) {
        // TODO Dev: cùng logic với Create — refactor sang common method dùng record/interface chung.
        throw new UnsupportedOperationException("FR-06A validateBlocking(Update) - to be implemented");
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, CreateEventRequest req) {
        // TODO Dev: trả về list, mỗi rule sai thêm 1 Warning.
        //   - WORKSHOP: req.startsAt > hackathon.registrationEnd → warn
        //   - KICKOFF: req.startsAt < hackathon.eventStart OR > hackathon.eventStart + 1d → warn
        //   - PRESENTATION: req.startsAt < lastKickoff.endsAt → warn
        //   - AWARDS: req.startsAt < max(PRESENTATION.startsAt) → warn
        return List.of();
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, UpdateEventRequest req) {
        return List.of();
    }
}
