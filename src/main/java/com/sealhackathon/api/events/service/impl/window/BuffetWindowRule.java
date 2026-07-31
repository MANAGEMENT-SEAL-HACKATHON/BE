package com.sealhackathon.api.events.service.impl.window;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BUFFET window — trong khoảng nghỉ [prelimEnd, final.examAt]
 * với {@code prelimEnd = examAt + codingDurationHours}.
 */
@Component
@RequiredArgsConstructor
public class BuffetWindowRule implements EventWindowRule {

    private final RoundRepository roundRepository;

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || h.getId() == null || startsAt == null) {
            return;
        }

        Round prelim = resolvePrelim(h.getId());
        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(h.getId()).orElse(null);
        if (prelim == null || finalRound == null
                || prelim.getExamAt() == null || finalRound.getExamAt() == null) {
            throw new BusinessRuleException(ErrorCode.EVENT_BUFFET_ROUNDS_MISSING,
                    "Buffet cần vòng Sơ loại và Chung kết đã có examAt",
                    Map.of("hackathonId", h.getId(),
                            "hasPrelim", prelim != null,
                            "hasFinal", finalRound != null));
        }

        int hours = prelim.getCodingDurationHours() != null && prelim.getCodingDurationHours() > 0
                ? prelim.getCodingDurationHours()
                : RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime breakStart = RoundScheduleSeedUtil.prelimEndAt(prelim.getExamAt(), hours);
        LocalDateTime breakEnd = finalRound.getExamAt();

        if (startsAt.isBefore(breakStart) || startsAt.isAfter(breakEnd)) {
            throw outOfBreak(h, startsAt, effectiveEnd, breakStart, breakEnd);
        }
        if (effectiveEnd != null
                && (effectiveEnd.isBefore(breakStart) || effectiveEnd.isAfter(breakEnd))) {
            throw outOfBreak(h, startsAt, effectiveEnd, breakStart, breakEnd);
        }
    }

    private Round resolvePrelim(Integer hackathonId) {
        List<Round> prelims = roundRepository.findPreliminaryLikeByHackathonId(hackathonId);
        if (!prelims.isEmpty()) {
            return prelims.get(0);
        }
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
    }

    private static BusinessRuleException outOfBreak(Hackathon h,
                                                    LocalDateTime startsAt,
                                                    LocalDateTime effectiveEnd,
                                                    LocalDateTime breakStart,
                                                    LocalDateTime breakEnd) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", "BUFFET");
        details.put("hackathonId", h.getId());
        details.put("startsAt", startsAt);
        details.put("effectiveEnd", effectiveEnd);
        details.put("breakStart", breakStart);
        details.put("breakEnd", breakEnd);
        return new BusinessRuleException(ErrorCode.EVENT_BUFFET_OUT_OF_BREAK,
                "Buffet phải nằm trong khung nghỉ [%s, %s]".formatted(breakStart, breakEnd),
                details);
    }
}
