package com.sealhackathon.api.rounds.guard;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.exception.ScoringLockedException;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Kiểm tra round tồn tại, active, chưa lock (tùy ngữ cảnh). */
@Component
@RequiredArgsConstructor
public class RoundAccessGuard {

    private final RoundRepository roundRepository;

    public Round requireRound(Integer roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
    }

    public Round requireActiveRound(Integer roundId) {
        Round round = requireRound(roundId);
        if (!Boolean.TRUE.equals(round.getIsActive())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_ACTIVE,
                    "Round chưa được kích hoạt",
                    Map.of("roundId", roundId));
        }
        return round;
    }

    /**
     * Active round with {@code PESSIMISTIC_WRITE} — dùng cho close-early / lock-scoring
     * để tránh Lost Update khi 2 Coord gọi song song.
     */
    public Round requireActiveRoundForUpdate(Integer roundId) {
        Round round = roundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (!Boolean.TRUE.equals(round.getIsActive())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_ACTIVE,
                    "Round chưa được kích hoạt",
                    Map.of("roundId", roundId));
        }
        return round;
    }

    public Round requireUnlockedRound(Integer roundId) {
        Round round = requireRound(roundId);
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new ScoringLockedException("Round đã khóa chấm điểm");
        }
        return round;
    }

    public Round requireActiveUnlockedRound(Integer roundId) {
        requireActiveRound(roundId);
        return requireUnlockedRound(roundId);
    }
}
