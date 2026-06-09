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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;

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

        // BỘ NÃO AUTO-LOTTERY NẾU FE KHÔNG GỬI ASSIGNMENTS
        if (req.getAssignments() == null || req.getAssignments().isEmpty()) {

            // 1. Lấy tất cả đội hợp lệ chưa bốc thăm
            List<Team> eligibleTeams = teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.ACTIVE).stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsLocked()))
                    .filter(t -> teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(t.getId(), round.getId()).isEmpty())
                    .collect(Collectors.toList());

            // 2. Lấy tất cả Track đang mở
            List<Track> openTracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                    .filter(t -> t.getStatus() == com.sealhackathon.api.tracks.value_object.TrackStatus.OPEN)
                    .collect(Collectors.toList());

            if (openTracks.isEmpty() && !eligibleTeams.isEmpty()) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Không có Track nào đang MỞ để bốc thăm");
            }

            // 3. Trộn ngẫu nhiên danh sách đội để đảm bảo công bằng
            Collections.shuffle(eligibleTeams);

            List<HackathonLotteryRequest.Assignment> autoAssignments = new ArrayList<>();
            int trackCount = openTracks.size();

            // 4. Phân bổ Track round-robin; Bảng = chữ La Mã theo thứ tự track (Track 1→A, 2→B, …)
            for (int i = 0; i < eligibleTeams.size(); i++) {
                Team team = eligibleTeams.get(i);
                int trackIdx = i % trackCount;
                Track track = openTracks.get(trackIdx);
                String assignedGroup = assignedGroupForTrackSequenceIndex(trackIdx);

                autoAssignments.add(HackathonLotteryRequest.Assignment.builder()
                        .teamId(team.getId())
                        .trackId(track.getId())
                        .assignedGroup(assignedGroup)
                        .build());
            }
            // Gán lại vào request để tiếp tục luồng lưu DB bên dưới
            req.setAssignments(autoAssignments);
        }

        User coordinator = null;
        if (currentUserAccessor.currentUserId() != null) {
            coordinator = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        }

        Map<Integer, Integer> trackSequenceIndex = buildTrackSequenceIndex(round.getId());

        int assignedCount = 0;
        List<Integer> teamIds = new ArrayList<>();
        List<HackathonLotteryResponse.AssignmentResult> results = new ArrayList<>();

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

            if (teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), round.getId()).isPresent()) {
                throw new ConflictException(ErrorCode.TEAM_ALREADY_IN_TRACK_THIS_ROUND, "Đội đã được phân vào một Track trong vòng này");
            }

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

            String assignedGroup = assignment.getAssignedGroup();
            if (!StringUtils.hasText(assignedGroup)) {
                Integer trackIdx = trackSequenceIndex.get(track.getId());
                if (trackIdx == null) {
                    throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                            "Track không nằm trong danh sách track OPEN của round");
                }
                assignedGroup = assignedGroupForTrackSequenceIndex(trackIdx);
            }

            TeamRoundTrack trt = TeamRoundTrack.builder()
                    .team(team)
                    .track(track)
                    .assignedGroup(assignedGroup)
                    .registrationType(com.sealhackathon.api.team_round_tracks.value_object.RegistrationType.ASSIGNED)
                    .assignedAt(LocalDateTime.now())
                    .assignedBy(coordinator)
                    .build();
            teamRoundTrackRepository.save(trt);

            auditService.log(AuditAction.TEAM_TRACK_ASSIGNED, "team_round_tracks", trt.getId(),
                    Map.of("teamId", team.getId(), "trackId", track.getId(), "roundId", round.getId()));

            assignedCount++;
            teamIds.add(team.getId());
            results.add(HackathonLotteryResponse.AssignmentResult.builder()
                    .teamId(team.getId())
                    .trackId(track.getId())
                    .trackName(track.getName())
                    .assignedGroup(assignedGroup)
                    .build());
        }

        return HackathonLotteryResponse.builder()
                .hackathonId(hackathonId)
                .roundId(round.getId())
                .assignedCount(assignedCount)
                .teamIds(teamIds)
                .assignments(results)
                .build();
    }

    /**
     * Số đội mỗi track sau khi shuffle + phân round-robin (team i → track {@code i % trackCount}).
     * Ví dụ 24 đội / 4 track → [6,6,6,6]; 23 đội / 4 track → [6,6,6,5].
     */
    static int[] roundRobinTeamCounts(int teamCount, int trackCount) {
        if (trackCount <= 0) {
            throw new IllegalArgumentException("trackCount phải > 0");
        }
        if (teamCount < 0) {
            throw new IllegalArgumentException("teamCount phải >= 0");
        }
        int[] counts = new int[trackCount];
        for (int i = 0; i < teamCount; i++) {
            counts[i % trackCount]++;
        }
        return counts;
    }

    /**
     * Track thứ N (0-based, theo {@code sequence_order}) đại diện Bảng La Mã thứ N:
     * Track 1 → Bảng A, Track 2 → Bảng B, Track 3 → Bảng C, …
     */
    static String assignedGroupForTrackSequenceIndex(int trackSequenceIndexZeroBased) {
        if (trackSequenceIndexZeroBased < 0 || trackSequenceIndexZeroBased > 25) {
            throw new IllegalArgumentException("trackSequenceIndex vượt quá A–Z: " + trackSequenceIndexZeroBased);
        }
        return "Bảng " + (char) ('A' + trackSequenceIndexZeroBased);
    }

    private Map<Integer, Integer> buildTrackSequenceIndex(Integer roundId) {
        List<Track> openTracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId).stream()
                .filter(t -> t.getStatus() == com.sealhackathon.api.tracks.value_object.TrackStatus.OPEN)
                .toList();
        Map<Integer, Integer> indexByTrackId = new HashMap<>();
        for (int i = 0; i < openTracks.size(); i++) {
            indexByTrackId.put(openTracks.get(i).getId(), i);
        }
        return indexByTrackId;
    }
}