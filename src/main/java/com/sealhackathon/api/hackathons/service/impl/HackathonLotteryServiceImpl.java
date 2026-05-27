package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.dto.request.HackathonLotteryRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonLotteryResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonLotteryService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MF-02 FR-13B — Bốc thăm Track (batch).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HackathonLotteryServiceImpl implements HackathonLotteryService {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Override
    public HackathonLotteryResponse runLottery(Integer hackathonId, HackathonLotteryRequest req) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Chỉ bốc thăm khi Hackathon đang ONGOING");
        }

        Round round = roundRepository.findById(req.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round", req.getRoundId()));

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND, "Không bốc thăm Track cho Vòng Chung Kết");
        }
        if (Boolean.TRUE.equals(round.getIsActive())) {
            throw new BusinessRuleException(ErrorCode.ROUND_ALREADY_ACTIVE, "Vòng thi đã kích hoạt, không thể bốc thăm");
        }

        User coordinator = null;
        if (currentUserAccessor.currentUserId() != null) {
            coordinator = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        }

        int assignedCount = 0;
        List<Integer> teamIds = new ArrayList<>();

        for (HackathonLotteryRequest.Assignment assignment : req.getAssignments()) {
            Team team = teamRepository.findById(assignment.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", assignment.getTeamId()));

            if (!team.getHackathon().getId().equals(hackathonId)) {
                throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION, "Đội không thuộc Hackathon này");
            }
            if (team.getStatus() != TeamStatus.ACTIVE) {
                throw new BusinessRuleException(ErrorCode.TEAM_NOT_ACTIVE, "Đội phải ở trạng thái ACTIVE mới được bốc thăm");
            }
            if (!Boolean.TRUE.equals(team.getIsLocked())) {
                throw new BusinessRuleException(ErrorCode.TEAM_NOT_LOCKED, "Đội chưa bị khóa (is_locked=false). Chỉ bốc thăm khi đã hết hạn đăng ký.");
            }

            Track track = trackRepository.findById(assignment.getTrackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Track", assignment.getTrackId()));

            if (!track.getRound().getId().equals(round.getId())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Track không thuộc Vòng thi này");
            }
            if (track.getStatus() != com.sealhackathon.api.tracks.value_object.TrackStatus.OPEN) {
                throw new BusinessRuleException(ErrorCode.TRACK_CLOSED, "Track đã đóng");
            }

            // Đội đã ở trong Track của Vòng này chưa?
            if (teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), round.getId()).isPresent()) {
                throw new ConflictException(ErrorCode.TEAM_ALREADY_IN_TRACK_THIS_ROUND, "Đội đã được phân vào một Track trong vòng này");
            }

            // BƯỚC 1: TẠO PARTICIPATION TRƯỚC (Yêu cầu v3.5)
            TeamRoundParticipation participation = teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), round.getId())
                    .orElse(null);

            if (participation == null) {
                participation = TeamRoundParticipation.builder()
                        .team(team)
                        .round(round)
                        .hackathon(hackathon)
                        .createdAt(LocalDateTime.now())
                        .build();
                teamRoundParticipationRepository.save(participation);
            }

            // BƯỚC 2: TẠO TEAM_ROUND_TRACKS
            TeamRoundTrack trt = TeamRoundTrack.builder()
                    .team(team)
                    .track(track)
                    .assignedGroup(assignment.getAssignedGroup())
                    .registrationType(com.sealhackathon.api.team_round_tracks.value_object.RegistrationType.ASSIGNED)
                    .assignedAt(LocalDateTime.now())
                    .assignedBy(coordinator)
                    .build();
            teamRoundTrackRepository.save(trt);

            auditService.log(AuditAction.TEAM_TRACK_ASSIGNED, "team_round_tracks", trt.getId(),
                    Map.of("teamId", team.getId(), "trackId", track.getId(), "roundId", round.getId()));

            assignedCount++;
            teamIds.add(team.getId());
        }

        return HackathonLotteryResponse.builder()
                .hackathonId(hackathonId)
                .roundId(round.getId())
                .assignedCount(assignedCount)
                .teamIds(teamIds)
                .build();
    }
}
