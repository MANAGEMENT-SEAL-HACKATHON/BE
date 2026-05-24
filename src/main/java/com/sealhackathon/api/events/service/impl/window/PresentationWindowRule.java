package com.sealhackathon.api.events.service.impl.window;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * FR-06A PRESENTATION window — optional event, dùng được cho cả Sơ loại lẫn Chung kết.
 *
 * <p>Rule:
 * <ul>
 *   <li>{@code date ∈ [eventStart, eventEnd]}</li>
 *   <li>Nếu Round Final ({@code isFinal=TRUE}) tồn tại với {@code examAt != null}:
 *       {@code startsAt > Final.examAt} — đảm bảo PRESENTATION xảy ra sau khi thi xong Chung kết</li>
 * </ul>
 *
 * <p>Layer 3 ordering ({@code WORKSHOP < KICKOFF < PRESENTATION < AWARDS}) đã enforce
 * ràng buộc "sau KICKOFF" trong {@code EventScheduleValidatorImpl.validateLayer3Ordering}.
 */
@Component
@RequiredArgsConstructor
public class PresentationWindowRule implements EventWindowRule {

    private final RoundRepository roundRepository;

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        LocalDate startDate = startsAt.toLocalDate();

        if (eventStart != null && startDate.isBefore(eventStart)) {
            throw fail(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "PRESENTATION phải trong khung Hackathon (từ %s)".formatted(eventStart),
                    h, startsAt, effectiveEnd);
        }
        if (eventEnd != null) {
            LocalDate effEndDate = (effectiveEnd != null ? effectiveEnd : startsAt).toLocalDate();
            if (effEndDate.isAfter(eventEnd)) {
                throw fail(ErrorCode.EVENT_OUT_OF_HACKATHON,
                        "PRESENTATION phải kết thúc trong khung Hackathon (đến %s)".formatted(eventEnd),
                        h, startsAt, effectiveEnd);
            }
        }

        Optional<Round> finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(h.getId());
        finalRound.ifPresent(round -> {
            LocalDateTime examAt = round.getExamAt();
            if (examAt != null && !startsAt.isAfter(examAt)) {
                Map<String, Object> details = new HashMap<>();
                details.put("type", "PRESENTATION");
                details.put("startsAt", startsAt);
                details.put("finalRoundId", round.getId());
                details.put("finalExamAt", examAt);
                throw new BusinessRuleException(ErrorCode.PRESENTATION_BEFORE_FINAL_EXAM,
                        "PRESENTATION (%s) phải sau ngày thi Chung kết (%s)".formatted(startsAt, examAt),
                        details);
            }
        });
    }

    private static BusinessRuleException fail(String code, String message, Hackathon h,
                                              LocalDateTime startsAt, LocalDateTime effectiveEnd) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", "PRESENTATION");
        details.put("eventStart", h.getEventStart());
        details.put("eventEnd", h.getEventEnd());
        details.put("startsAt", startsAt);
        details.put("effectiveEnd", effectiveEnd);
        return new BusinessRuleException(code, message, details);
    }
}
