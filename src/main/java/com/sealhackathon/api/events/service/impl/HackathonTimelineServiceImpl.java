package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HackathonTimelineServiceImpl implements HackathonTimelineService {

    private final EventRepository eventRepository;
    private final RoundRepository roundRepository;
    private final HackathonRepository hackathonRepository;

    @Override
    public void validateRoundExamAt(Integer hackathonId, boolean isFinal, LocalDateTime examAt) {
        if (examAt == null) {
            return;
        }
        validateExamAtAgainstEvents(hackathonId, isFinal, examAt);
    }

    @Override
    public List<BusinessRuleException> collectRoundExamAtViolations(Integer hackathonId) {
        List<BusinessRuleException> violations = new ArrayList<>();
        for (Round round : roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId)) {
            if (round.getExamAt() == null) {
                continue;
            }
            try {
                validateExamAtAgainstEvents(hackathonId,
                        Boolean.TRUE.equals(round.getIsFinal()),
                        round.getExamAt());
            } catch (BusinessRuleException ex) {
                violations.add(wrapRoundContext(ex, round));
            }
        }
        return violations;
    }

    @Override
    public void assertAllRoundsExamAtValid(Integer hackathonId) {
        List<BusinessRuleException> violations = collectRoundExamAtViolations(hackathonId);
        if (!violations.isEmpty()) {
            throw violations.get(0);
        }
    }

    private static BusinessRuleException wrapRoundContext(BusinessRuleException ex, Round round) {
        Map<String, Object> details = new java.util.LinkedHashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("roundId", round.getId());
        details.put("roundName", round.getName());
        details.put("examAt", round.getExamAt());
        return new BusinessRuleException(ex.getCode(),
                "Round '%s' (id=%d): %s".formatted(round.getName(), round.getId(), ex.getMessage()),
                details);
    }

    private void validateExamAtAgainstEvents(Integer hackathonId, boolean isFinal, LocalDateTime examAt) {
        eventRepository.findLatestByType(hackathonId, EventType.KICKOFF).stream()
                .map(EventTimeline::effectiveEnd)
                .max(LocalDateTime::compareTo)
                .ifPresent(kickoffEnd -> {
                    if (!examAt.isAfter(kickoffEnd)) {
                        throw new BusinessRuleException(ErrorCode.ROUND_EXAM_BEFORE_KICKOFF,
                                "Ngày thi (%s) phải sau khi Khai mạc kết thúc (%s)"
                                        .formatted(examAt, kickoffEnd),
                                Map.of("hackathonId", hackathonId,
                                        "examAt", examAt,
                                        "kickoffEndsAt", kickoffEnd));
                    }
                });

        // FR-06A v3.2 — examAt phải nằm trong khung [eventStart, eventEnd] (inclusive ngày).
        Optional<Hackathon> maybeHackathon = hackathonRepository.findById(hackathonId);
        maybeHackathon.ifPresent(h -> assertExamAtWithinEventWindow(h, examAt));

        if (isFinal) {
            List<Event> awardsEvents = eventRepository.findByHackathonIdAndType(
                    hackathonId, EventType.AWARDS);
            if (awardsEvents.isEmpty()) {
                throw new BusinessRuleException(ErrorCode.EVENT_AWARDS_MISSING,
                        "Round Chung kết cần sự kiện AWARDS — tạo lễ trao giải trước khi đặt examAt",
                        Map.of("hackathonId", hackathonId, "examAt", examAt));
            }
            // FR-06A v3.2 — Final.examAt phải xảy ra TRƯỚC AWARDS.startsAt
            // (đảo so với rule cũ "phải trong khung AWARDS"). Lý do: AWARDS giờ đứng cuối timeline,
            // sau khi đã thi xong; xem AwardsWindowRule.
            for (Event awards : awardsEvents) {
                LocalDateTime awardsStart = awards.getStartsAt();
                if (awardsStart == null) {
                    continue;
                }
                if (!examAt.isBefore(awardsStart)) {
                    throw new BusinessRuleException(ErrorCode.ROUND_EXAM_OUTSIDE_AWARDS,
                            "Ngày thi Chung kết (%s) phải TRƯỚC Lễ trao giải bắt đầu (%s)"
                                    .formatted(examAt, awardsStart),
                            Map.of("hackathonId", hackathonId,
                                    "examAt", examAt,
                                    "awardsStartsAt", awardsStart,
                                    "awardsEventId", awards.getId()));
                }
            }
            return;
        }

        // FR-06A v3.2 — PRESENTATION optional cho Sơ loại.
        // Nếu BTC chưa tạo PRESENTATION thì examAt vẫn hợp lệ miễn là nằm trong [eventStart, eventEnd].
        // Khi có PRESENTATION → examAt phải nằm trong khung PRESENTATION.
        List<Event> presentationEvents = eventRepository.findByHackathonIdAndType(
                hackathonId, EventType.PRESENTATION);
        for (Event presentation : presentationEvents) {
            LocalDateTime presStart = presentation.getStartsAt();
            LocalDateTime presEnd = EventTimeline.effectiveEnd(presentation);
            if (presStart == null) {
                continue;
            }
            if (examAt.isBefore(presStart) || examAt.isAfter(presEnd)) {
                throw new BusinessRuleException(ErrorCode.ROUND_EXAM_OUTSIDE_PRESENTATION,
                        "Ngày thi Sơ loại (%s) phải trong khung Ngày thi (%s – %s)"
                                .formatted(examAt, presStart, presEnd),
                        Map.of("hackathonId", hackathonId,
                                "examAt", examAt,
                                "presentationStartsAt", presStart,
                                "presentationEndsAt", presEnd,
                                "presentationEventId", presentation.getId()));
            }
        }
    }

    private static void assertExamAtWithinEventWindow(Hackathon h, LocalDateTime examAt) {
        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        LocalDate examDate = examAt.toLocalDate();
        if (eventStart != null && examDate.isBefore(eventStart)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Ngày thi (%s) trước eventStart (%s)".formatted(examDate, eventStart),
                    Map.of("hackathonId", h.getId(),
                            "examAt", examAt,
                            "eventStart", eventStart));
        }
        if (eventEnd != null && examDate.isAfter(eventEnd)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Ngày thi (%s) sau eventEnd (%s)".formatted(examDate, eventEnd),
                    Map.of("hackathonId", h.getId(),
                            "examAt", examAt,
                            "eventEnd", eventEnd));
        }
    }
}
