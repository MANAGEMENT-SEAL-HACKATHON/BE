package com.sealhackathon.api.events.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Chặn sửa/xóa sự kiện BUFFET và thực đơn sau khi sơ loại đã công bố kết quả.
 */
@Component
@RequiredArgsConstructor
public class BuffetEditGuard {

    private final RoundRepository roundRepository;

    public void assertEditable(Integer hackathonId) {
        if (hackathonId == null) {
            return;
        }
        Round prelim = resolvePrelim(hackathonId);
        if (prelim != null && Boolean.TRUE.equals(prelim.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.BUFFET_LOCKED_AFTER_PUBLISH,
                    "Không thể sửa buffet sau khi đã công bố kết quả sơ loại",
                    Map.of("hackathonId", hackathonId,
                            "prelimRoundId", prelim.getId()));
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
}
