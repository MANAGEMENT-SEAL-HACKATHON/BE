package com.sealhackathon.api.events.service.impl.window;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FR-06A AWARDS window — Lễ trao giải đứng cuối timeline.
 *
 * <p>Rule:
 * <ul>
 *   <li>{@code date == eventEnd} (đúng ngày kết thúc Hackathon)</li>
 *   <li>Phải có ≥1 mốc thi: PRESENTATION đã tạo HOẶC ≥1 Round có {@code examAt != null} —
 *       không cho tạo AWARDS khi chưa có dấu hiệu cuộc thi đã/đang diễn ra
 *       ({@link ErrorCode#AWARDS_NEEDS_COMPETITION})</li>
 *   <li>{@code startsAt > max(PRESENTATION.endsAt, all Round.examAt)} —
 *       lễ trao giải phải sau khi đã thi xong
 *       ({@link ErrorCode#AWARDS_BEFORE_COMPETITION_END})</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AwardsWindowRule implements EventWindowRule {

    private final EventRepository eventRepository;
    private final RoundRepository roundRepository;

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        LocalDate eventEnd = h.getEventEnd();
        LocalDate startDate = startsAt.toLocalDate();

        if (eventEnd != null && !startDate.equals(eventEnd)) {
            Map<String, Object> details = new HashMap<>();
            details.put("type", "AWARDS");
            details.put("eventEnd", eventEnd);
            details.put("startsAt", startsAt);
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "AWARDS phải đúng ngày kết thúc Hackathon (%s)".formatted(eventEnd),
                    details);
        }
        if (effectiveEnd != null && eventEnd != null
                && effectiveEnd.toLocalDate().isAfter(eventEnd)) {
            Map<String, Object> details = new HashMap<>();
            details.put("type", "AWARDS");
            details.put("eventEnd", eventEnd);
            details.put("effectiveEnd", effectiveEnd);
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "AWARDS phải kết thúc trong ngày eventEnd (%s)".formatted(eventEnd),
                    details);
        }

        LocalDateTime presentationEnd = latestPresentationEnd(h.getId(), excludeEventId);
        LocalDateTime maxRoundExam = latestRoundExamAt(h.getId());

        if (presentationEnd == null && maxRoundExam == null) {
            Map<String, Object> details = new HashMap<>();
            details.put("hackathonId", h.getId());
            details.put("startsAt", startsAt);
            throw new BusinessRuleException(ErrorCode.AWARDS_NEEDS_COMPETITION,
                    "Cần có ít nhất 1 sự kiện thi (PRESENTATION hoặc Round với ngày thi) "
                            + "trước khi tạo Lễ trao giải",
                    details);
        }

        LocalDateTime competitionEnd = max(presentationEnd, maxRoundExam);
        if (!startsAt.isAfter(competitionEnd)) {
            Map<String, Object> details = new HashMap<>();
            details.put("hackathonId", h.getId());
            details.put("startsAt", startsAt);
            details.put("competitionEnd", competitionEnd);
            details.put("presentationEnd", presentationEnd);
            details.put("maxRoundExamAt", maxRoundExam);
            throw new BusinessRuleException(ErrorCode.AWARDS_BEFORE_COMPETITION_END,
                    "AWARDS (%s) phải sau khi thi xong (mốc cuối: %s)"
                            .formatted(startsAt, competitionEnd),
                    details);
        }
    }

    private LocalDateTime latestPresentationEnd(Integer hackathonId, Integer excludeEventId) {
        int ex = excludeEventId == null ? 0 : excludeEventId;
        List<Event> events = eventRepository.findByHackathonIdAndType(hackathonId, EventType.PRESENTATION);
        LocalDateTime latest = null;
        for (Event e : events) {
            if (e.getId() != null && e.getId() == ex) {
                continue;
            }
            LocalDateTime end = EventTimeline.effectiveEnd(e);
            if (end == null) {
                continue;
            }
            if (latest == null || end.isAfter(latest)) {
                latest = end;
            }
        }
        return latest;
    }

    private LocalDateTime latestRoundExamAt(Integer hackathonId) {
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        LocalDateTime latest = null;
        for (Round r : rounds) {
            LocalDateTime examAt = r.getExamAt();
            if (examAt == null) {
                continue;
            }
            if (latest == null || examAt.isAfter(latest)) {
                latest = examAt;
            }
        }
        return latest;
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
