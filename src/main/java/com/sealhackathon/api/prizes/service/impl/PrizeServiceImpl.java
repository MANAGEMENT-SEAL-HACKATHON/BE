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
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.mapper.PrizeMapper;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.service.PrizeService;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrizeServiceImpl implements PrizeService {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
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
    @Transactional(readOnly = true)
    public List<PrizeResponse> listByHackathon(Integer hackathonId) {
        // TODO: FR-32 — list prizes; gate PENDING_CONFIRM+
        return Collections.emptyList();
    }

    @Override
    public void revoke(Integer prizeId) {
        // TODO: FR-32 — chặn nếu hackathon FINISHED; DELETE + audit FR-36
    }

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
