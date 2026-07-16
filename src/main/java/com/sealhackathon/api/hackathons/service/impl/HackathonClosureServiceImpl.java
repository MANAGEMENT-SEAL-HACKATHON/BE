package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.dto.request.ConfirmHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.event.HackathonFinishedEvent;
import com.sealhackathon.api.hackathons.mapper.HackathonMapper;
import com.sealhackathon.api.hackathons.query.FinalRankingQueryService;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonClosureService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class HackathonClosureServiceImpl implements HackathonClosureService {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final RoundRankingQueryService roundRankingQueryService;
    private final RoundProgressionService roundProgressionService;
    private final PrizeRepository prizeRepository;
    private final HackathonMapper hackathonMapper;
    private final FinalRankingQueryService finalRankingQueryService;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public HackathonResponse confirm(Integer hackathonId, ConfirmHackathonRequest req) {
        if (!Boolean.TRUE.equals(req.getConfirm())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "confirm phải là true để chốt kết quả");
        }

        Hackathon hackathon = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_PENDING_CONFIRM,
                    "Chỉ confirm khi hackathon ở trạng thái PENDING_CONFIRM",
                    Map.of("hackathonId", hackathonId, "status", hackathon.getStatus().name()));
        }

        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.MISSING_FINAL_ROUND,
                        "Thiếu Round Chung kết", Map.of("hackathonId", hackathonId)));

        if (!Boolean.TRUE.equals(finalRound.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_NOT_SCORING_LOCKED,
                    "Phải khóa chấm Chung kết trước khi confirm",
                    Map.of("finalRoundId", finalRound.getId()));
        }

        if (roundRankingQueryService.hasIncompleteScoring(finalRound.getId(), false)) {
            throw new BusinessRuleException(ErrorCode.SCORING_INCOMPLETE_BEFORE_CONFIRM,
                    "Chưa chấm đủ điểm Chung kết — không thể confirm kết quả",
                    Map.of("finalRoundId", finalRound.getId()));
        }

        // Gate Confirm theo round Chung kết còn DEEP_TIE (không khóa cả hackathon vì prelim khác)
        List<TiebreakItemResponse> unresolvedFinalTiebreaks = roundProgressionService.tiebreak(finalRound.getId());
        if (!unresolvedFinalTiebreaks.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.TIEBREAK_UNRESOLVED,
                    "Vòng Chung kết còn đồng điểm chưa resolve — không thể Confirm FINISHED",
                    Map.of("finalRoundId", finalRound.getId(), "unresolvedCount", unresolvedFinalTiebreaks.size()));
        }

        if (prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(hackathonId).isEmpty()) {
            throw new BusinessRuleException(ErrorCode.NO_PRIZES_RECORDED,
                    "Chưa ghi nhận giải thưởng — cần ít nhất 1 prize trước khi confirm",
                    Map.of("hackathonId", hackathonId));
        }

        HackathonStatus from = hackathon.getStatus();
        hackathon.setStatus(HackathonStatus.FINISHED);
        Hackathon saved = hackathonRepository.save(hackathon);

        java.util.HashMap<String, Object> auditMeta = new java.util.HashMap<>();
        auditMeta.put("from", from.name());
        auditMeta.put("to", HackathonStatus.FINISHED.name());
        auditMeta.put("note", req.getNote() != null ? req.getNote() : "");
        auditMeta.put("validatedBy", currentUserAccessor.currentUserId());
        auditMeta.put("validatedAt", LocalDateTime.now().toString());
        auditMeta.put("via", "confirm");
        auditService.log(AuditAction.HACKATHON_STATUS_CHANGE, "hackathons", saved.getId(), auditMeta);

        eventPublisher.publishEvent(new HackathonFinishedEvent(this, hackathonId));

        return hackathonMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinalTeamRankingItemResponse> teamRankings(Integer hackathonId) {
        return finalRankingQueryService.teamRankingsForHackathon(hackathonId);
    }
}
