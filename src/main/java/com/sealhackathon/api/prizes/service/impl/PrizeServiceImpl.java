package com.sealhackathon.api.prizes.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.dto.request.AwardPrizeRequest;
import com.sealhackathon.api.prizes.dto.request.RevokePrizeRequest;
import com.sealhackathon.api.prizes.dto.request.UpdateAwardedPrizeRequest;
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.mapper.PrizeMapper;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.service.PrizeService;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrizeServiceImpl implements PrizeService {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final RoundProgressionService roundProgressionService;
    private final TeamRepository teamRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TrackRepository trackRepository;
    private final PrizeRepository prizeRepository;
    private final UserRepository userRepository;
    private final PrizeMapper prizeMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    public PrizeResponse award(Integer hackathonId, AwardPrizeRequest req) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() == HackathonStatus.FINISHED) {
            throw new ConflictException(ErrorCode.HACKATHON_ARCHIVED,
                    "Hackathon đã kết thúc — không thể trao giải mới",
                    Map.of("hackathonId", hackathonId, "status", hackathon.getStatus().name()));
        }
        if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_PENDING_CONFIRM,
                    "Chỉ trao giải khi hackathon ở trạng thái PENDING_CONFIRM",
                    Map.of("hackathonId", hackathonId, "status", hackathon.getStatus().name()));
        }

        Round round = roundRepository.findById(req.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round", req.getRoundId()));
        if (!round.getHackathon().getId().equals(hackathonId)) {
            throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION,
                    "Round không thuộc hackathon này",
                    Map.of("hackathonId", hackathonId, "roundId", req.getRoundId()));
        }

        Team team = teamRepository.findById(req.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", req.getTeamId()));
        if (!team.getHackathon().getId().equals(hackathonId)) {
            throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION,
                    "Team không thuộc hackathon này",
                    Map.of("hackathonId", hackathonId, "teamId", req.getTeamId()));
        }

        assertFinalistEligible(hackathonId, round, team);

        Track track = null;
        if (req.getTrackId() != null) {
            track = trackRepository.findById(req.getTrackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Track", req.getTrackId()));
            if (!track.getRound().getId().equals(round.getId())) {
                throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION,
                        "Track không thuộc round đã chọn",
                        Map.of("roundId", round.getId(), "trackId", req.getTrackId()));
            }
        }

        // Gate awards theo round đang trao giải còn DEEP_TIE
        List<TiebreakItemResponse> unresolved = roundProgressionService.tiebreak(round.getId());
        if (!unresolved.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.TIEBREAK_UNRESOLVED,
                    "Round còn đồng điểm chưa resolve — không thể trao giải cho round này",
                    Map.of("roundId", round.getId(), "unresolvedCount", unresolved.size()));
        }

        assertNoDuplicate(hackathonId, req.getRoundId(), req.getTeamId(), req.getPrizeRank());

        User awarder = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));

        Prize saved = prizeRepository.save(Prize.builder()
                .hackathon(hackathon)
                .round(round)
                .track(track)
                .team(team)
                .prizeName(req.getPrizeName())
                .prizeRank(req.getPrizeRank())
                .prizeValue(req.getPrizeValue())
                .description(req.getDescription())
                .awardedBy(awarder)
                .build());

        auditService.log(AuditAction.PRIZE_AWARDED, "prizes", saved.getId(), Map.of(
                "hackathonId", hackathonId,
                "roundId", round.getId(),
                "teamId", team.getId(),
                "prizeRank", req.getPrizeRank() != null ? req.getPrizeRank().name() : null));

        return prizeMapper.toResponse(saved);
    }

    @Override
    public PrizeResponse updateAwarded(Integer prizeId, UpdateAwardedPrizeRequest req) {
        Prize prize = prizeRepository.findById(prizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Prize", prizeId));
        Hackathon hackathon = prize.getHackathon();
        if (hackathon.getStatus() == HackathonStatus.FINISHED) {
            throw new ConflictException(ErrorCode.HACKATHON_ARCHIVED,
                    "Hackathon đã kết thúc — không thể sửa giải đã trao",
                    Map.of("hackathonId", hackathon.getId()));
        }
        Integer oldTeamId = prize.getTeam().getId();
        if (req.getPrizeName() != null && !req.getPrizeName().isBlank()) {
            prize.setPrizeName(req.getPrizeName().trim());
        }
        if (req.getTeamId() != null && !req.getTeamId().equals(oldTeamId)) {
            Team newTeam = teamRepository.findById(req.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", req.getTeamId()));
            if (!newTeam.getHackathon().getId().equals(hackathon.getId())) {
                throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION,
                        "Team không thuộc hackathon này",
                        Map.of("hackathonId", hackathon.getId(), "teamId", req.getTeamId()));
            }
            assertFinalistEligible(hackathon.getId(), prize.getRound(), newTeam);
            // PRIZE-02: 1 đội ≤ 1 giải chính (re-validate on reassignment)
            if (prizeRepository.existsByHackathonIdAndTeamId(hackathon.getId(), newTeam.getId())) {
                throw new ConflictException(ErrorCode.PRIZE_DUPLICATE,
                        "Đội mới đã có giải chính trong hackathon này",
                        Map.of("hackathonId", hackathon.getId(), "teamId", newTeam.getId()));
            }
            prize.setTeam(newTeam);
        }
        Prize saved = prizeRepository.save(prize);
        java.util.HashMap<String, Object> auditMeta = new java.util.HashMap<>();
        auditMeta.put("reason", req.getReason() != null ? req.getReason() : "");
        auditMeta.put("oldTeamId", oldTeamId);
        auditMeta.put("newTeamId", saved.getTeam().getId());
        auditMeta.put("prizeName", saved.getPrizeName());
        auditService.log(AuditAction.PRIZE_AWARD_UPDATED, "prizes", prizeId, auditMeta);
        return prizeMapper.toResponse(saved);
    }

    // LOGIC LẤY DANH SÁCH GIẢI THƯỞNG
    @Override
    @Transactional(readOnly = true)
    public List<PrizeResponse> listByHackathon(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        // Gate: Chỉ được xem giải khi Hackathon đang chờ xác nhận hoặc đã kết thúc
        if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM && hackathon.getStatus() != HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ có thể xem danh sách giải thưởng khi Hackathon ở trạng thái PENDING_CONFIRM hoặc FINISHED");
        }

        return prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(hackathonId)
                .stream()
                .map(prizeMapper::toResponse)
                .toList();
    }

    // LOGIC THU HỒI GIẢI THƯỞNG — category + note bắt buộc (ngang Wildcard Override)
    @Override
    public void revoke(Integer prizeId, RevokePrizeRequest req) {
        if (req == null
                || !StringUtils.hasText(req.getCategory())
                || !StringUtils.hasText(req.getNote())) {
            throw new BusinessRuleException(ErrorCode.PRIZE_REVOKE_REASON_REQUIRED,
                    "Thu hồi giải bắt buộc chọn lý do (category) và ghi chú.");
        }
        String category = req.getCategory().trim().toUpperCase();
        if (!ALLOWED_REVOKE_CATEGORIES.contains(category)) {
            throw new BusinessRuleException(ErrorCode.PRIZE_REVOKE_CATEGORY_INVALID,
                    "Category thu hồi không hợp lệ: " + category);
        }

        Prize prize = prizeRepository.findById(prizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Prize", prizeId));

        Hackathon hackathon = prize.getHackathon();

        // Chặn cứng: Không được phép thu hồi nếu Hackathon đã hạ màn
        if (hackathon.getStatus() == HackathonStatus.FINISHED) {
            throw new ConflictException(ErrorCode.HACKATHON_ARCHIVED,
                    "Hackathon đã kết thúc — không thể thu hồi giải thưởng",
                    Map.of("hackathonId", hackathon.getId(), "status", hackathon.getStatus().name()));
        }

        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("hackathonId", hackathon.getId());
        auditDetails.put("teamId", prize.getTeam().getId());
        auditDetails.put("prizeName", prize.getPrizeName());
        auditDetails.put("prizeRank", prize.getPrizeRank() != null ? prize.getPrizeRank().name() : "NONE");
        auditDetails.put("revokeCategory", category);
        auditDetails.put("revokeNote", req.getNote().trim());

        prizeRepository.delete(prize);

        auditService.log(AuditAction.PRIZE_REVOKED, "prizes", prizeId, auditDetails);
    }

    private void assertFinalistEligible(Integer hackathonId, Round awardRound, Team team) {
        if (team.getStatus() == TeamStatus.ELIMINATED) {
            throw new BusinessRuleException(ErrorCode.PRIZE_TEAM_NOT_FINALIST,
                    "Đội đã bị loại — không thể trao giải",
                    Map.of("hackathonId", hackathonId, "teamId", team.getId()));
        }
        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .orElse(null);
        if (finalRound == null) {
            throw new BusinessRuleException(ErrorCode.PRIZE_TEAM_NOT_FINALIST,
                    "Hackathon chưa có vòng Chung kết — không thể trao giải",
                    Map.of("hackathonId", hackathonId));
        }
        if (!Boolean.TRUE.equals(awardRound.getIsFinal())
                || !awardRound.getId().equals(finalRound.getId())) {
            throw new BusinessRuleException(ErrorCode.PRIZE_TEAM_NOT_FINALIST,
                    "Chỉ trao giải trên vòng Chung kết",
                    Map.of("hackathonId", hackathonId, "roundId", awardRound.getId()));
        }
        boolean inFinal = teamRoundParticipationRepository
                .findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                .isPresent();
        if (!inFinal) {
            throw new BusinessRuleException(ErrorCode.PRIZE_TEAM_NOT_FINALIST,
                    "Chỉ trao giải cho đội vào Chung kết",
                    Map.of("hackathonId", hackathonId, "teamId", team.getId(), "finalRoundId", finalRound.getId()));
        }
    }

    private static final java.util.Set<String> ALLOWED_REVOKE_CATEGORIES = java.util.Set.of(
            "AWARDED_IN_ERROR", "TEAM_DQ", "DUPLICATE_AWARD", "OTHER");

    private void assertNoDuplicate(Integer hackathonId, Integer roundId, Integer teamId, PrizeRank prizeRank) {
        if (prizeRepository.existsByRound_IdAndTeam_Id(roundId, teamId)
                || prizeRepository.existsByHackathonIdAndTeamId(hackathonId, teamId)) {
            throw new ConflictException(ErrorCode.PRIZE_DUPLICATE,
                    "Đội đã được trao giải trong hackathon này",
                    Map.of("hackathonId", hackathonId, "teamId", teamId));
        }
        if (prizeRank != null
                && (prizeRepository.existsByRound_IdAndPrizeRank(roundId, prizeRank)
                || prizeRepository.existsByHackathonIdAndPrizeRank(hackathonId, prizeRank))) {
            throw new ConflictException(ErrorCode.PRIZE_DUPLICATE,
                    "Loại giải đã được trao trong hackathon này",
                    Map.of("hackathonId", hackathonId, "prizeRank", prizeRank.name()));
        }
    }
}