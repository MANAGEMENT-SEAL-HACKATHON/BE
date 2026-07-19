package com.sealhackathon.api.tracks.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.support.HackathonTeamSizeResolver;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.tracks.dto.request.CreateTrackRequest;
import com.sealhackathon.api.tracks.dto.request.UpdateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.service.TrackService;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.storage.StoredObjectResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TrackServiceImpl implements TrackService {

    private static final Set<HackathonStatus> MUTABLE_PARENT = EnumSet.of(
            HackathonStatus.DRAFT, HackathonStatus.ONGOING);

    private static final Set<TeamStatus> ACTIVE_OR_PENDING_TEAM = EnumSet.of(
            TeamStatus.PENDING, TeamStatus.ACTIVE);

    private final TrackRepository trackRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackMapper trackMapper;
    private final AuditService auditService;
    private final TeamRepository teamRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final NotificationService notificationService;
    private final EventRepository eventRepository;
    private final CriteriaRepository criteriaRepository;
    private final HackathonArchiveGuard archiveGuard;
    private final TrackProblemStatementStorage trackProblemStatementStorage;
    private final HackathonTeamSizeResolver teamSizeResolver;
    private final RoundAccessGuard roundAccessGuard;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public TrackResponse createByRound(Integer roundId, CreateTrackRequest req) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                    "Không thể tạo Track trong Round Chung kết",
                    Map.of("roundId", roundId));
        }
        Hackathon h = round.getHackathon();
        guardParentStatus(h);
        validateSizes(req.getMinTeamSize(), req.getMaxTeamSize(),
                req.getMaxTeamsPerGroup(), req.getMaxTeams());
        teamSizeResolver.assertCompatibleWithExistingTracks(h.getId(),
                req.getMinTeamSize(), req.getMaxTeamSize(), null);

        int sequenceOrder = resolveSequenceOrder(roundId, req.getSequenceOrder());
        Track entity = trackMapper.toEntity(req, round, sequenceOrder);
        Track saved = trackRepository.save(entity);

        TrackResponse response = trackMapper.toResponse(saved);
        auditService.log(AuditAction.TRACK_CREATE, "tracks", saved.getId(),
                Map.of("roundId", roundId, "hackathonId", h.getId(), "snapshot", response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackSummaryResponse> listByHackathon(Integer hackathonId, TrackStatus statusFilter) {
        if (!hackathonRepository.existsById(hackathonId)) {
            throw new ResourceNotFoundException("Hackathon", hackathonId);
        }
        List<Track> tracks = (statusFilter == null)
                ? trackRepository.findByHackathonIdOrderById(hackathonId)
                : trackRepository.findByHackathonIdAndStatus(hackathonId, statusFilter);
        return tracks.stream().map(trackMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackSummaryResponse> listByRound(Integer roundId, TrackStatus statusFilter) {
        roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId);
        if (statusFilter != null) {
            tracks = tracks.stream()
                    .filter(t -> t.getStatus() == statusFilter)
                    .toList();
        }
        return tracks.stream().map(trackMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackResponse getById(Integer id) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
        return trackMapper.toResponse(t);
    }

    @Override
    public UpdateResult update(Integer id, UpdateTrackRequest req) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
        Hackathon parent = t.getRound() != null ? t.getRound().getHackathon() : null;
        guardParentStatus(parent);
        validateSizes(req.getMinTeamSize(), req.getMaxTeamSize(),
                req.getMaxTeamsPerGroup(), req.getMaxTeams());
        if (parent != null) {
            teamSizeResolver.assertCompatibleWithExistingTracks(parent.getId(),
                    req.getMinTeamSize(), req.getMaxTeamSize(), t.getId());
        }
        validateTopicAfterKickoff(t, parent, req.getTopic());

        TrackResponse before = trackMapper.toResponse(t);
        TrackStatus oldStatus = t.getStatus();
        if (req.getStatus() == TrackStatus.CANCELLED && oldStatus != TrackStatus.CANCELLED) {
            long activeTeams = teamRepository.countActiveByTrackId(id, ACTIVE_OR_PENDING_TEAM);
            if (activeTeams > 0) {
                throw new BusinessRuleException(ErrorCode.TRACK_CANCEL_HAS_TEAMS,
                        "Không thể hủy Track khi còn đội được phân công",
                        Map.of("trackId", id, "teamsActive", activeTeams));
            }
        }

        trackMapper.applyUpdate(t, req);
        Track saved = trackRepository.save(t);
        TrackResponse after = trackMapper.toResponse(saved);

        boolean topicChanged = req.getTopic() != null && !req.getTopic().trim().isEmpty()
                && !req.getTopic().trim().equals(
                before.getTopic() == null ? "" : before.getTopic().trim());
        if (topicChanged) {
            Map<String, Object> topicAudit = new HashMap<>();
            topicAudit.put("oldTopic", before.getTopic());
            topicAudit.put("newTopic", after.getTopic());
            auditService.log(AuditAction.TRACK_TOPIC_UPDATE, "tracks", saved.getId(), topicAudit);
        } else {
            auditService.logBeforeAfter(AuditAction.TRACK_UPDATE, "tracks", saved.getId(), before, after);
        }
        return new UpdateResult(after, List.of());
    }

    @Override
    public Integer delete(Integer id) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
        Hackathon parent = t.getRound() != null ? t.getRound().getHackathon() : null;
        guardParentStatus(parent);
        if (teamRepository.countActiveByTrackId(id, ACTIVE_OR_PENDING_TEAM) > 0) {
            throw new ConflictException(ErrorCode.TRACK_HAS_TEAMS,
                    "Track còn team đang đăng ký",
                    Map.of("trackId", id));
        }
        if (t.getRound() != null && Boolean.TRUE.equals(t.getRound().getIsActive())) {
            throw new ConflictException(ErrorCode.TRACK_HAS_ACTIVE_ROUND,
                    "Round cha đang active — không thể xóa Track",
                    Map.of("trackId", id));
        }

        TrackResponse snapshot = trackMapper.toResponse(t);
        List<MentorAssignment> mentors = mentorAssignmentRepository.findByTrackId(id);
        for (MentorAssignment ma : mentors) {
            notificationService.send(ma.getMentor(), "MENTOR_UNASSIGNED",
                    "Track '%s' đã bị xóa".formatted(t.getName()),
                    "Bạn không còn là Mentor của Track này do Track bị xóa.",
                    "tracks", id);
        }
        List<JudgeAssignment> judges = judgeAssignmentRepository.findByTrackId(id);
        for (JudgeAssignment ja : judges) {
            notificationService.send(ja.getJudge(), "JUDGE_UNASSIGNED",
                    "Track '%s' đã bị xóa".formatted(t.getName()),
                    "Bạn không còn là Judge của Track này do Track bị xóa.",
                    "tracks", id);
        }
        if (!judges.isEmpty()) {
            judgeAssignmentRepository.deleteByTrackId(id);
        }
        // Criteria: DB fk_criteria_track ON DELETE CASCADE — không chặn xóa Track vì còn criterion cấu hình.
        trackRepository.delete(t);
        auditService.log(AuditAction.TRACK_DELETE, "tracks", id,
                Map.of("snapshot", snapshot, "mentorCount", mentors.size(), "judgeCount", judges.size()));
        return id;
    }

    @Override
    public TrackResponse uploadProblemStatement(Integer id, MultipartFile file) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
        Round round = track.getRound();
        if (round == null || Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                    "Upload đề bài chỉ áp dụng cho bảng đấu vòng Sơ loại");
        }
        guardParentStatus(round.getHackathon());
        if (track.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đề bài đã được phát cho bảng đấu này — không thể thay file");
        }
        if (round.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đề bài đã được phát — không thể thay file trên bảng đấu");
        }
        trackProblemStatementStorage.store(track, file);
        Track saved = trackRepository.save(track);
        return trackMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadProblemStatement(Integer id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
        String filename = TrackProblemStatementStorage.displayFilename(track);
        return StoredObjectResource.toResource(trackProblemStatementStorage.load(track), filename);
    }

    @Override
    public TrackResponse releaseProblem(Integer trackId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        Round round = track.getRound();
        if (round == null || Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                    "Phát đề theo bảng đấu chỉ áp dụng cho vòng Sơ loại");
        }
        roundAccessGuard.requireActiveRound(round.getId());
        guardParentStatus(round.getHackathon());
        if (track.getStatus() == TrackStatus.CANCELLED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Bảng đấu đã CANCELLED — không thể phát đề");
        }
        if (track.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đề bài đã được phát cho bảng đấu này");
        }
        if (round.getProblemReleasedAt() != null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Vòng thi đã phát đề toàn bộ — không cần phát từng bảng đấu");
        }
        LocalDateTime now = LocalDateTime.now();
        if (round.getExamAt() == null || round.getExamAt().isAfter(now)) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_BEFORE_EXAM,
                    "Chưa tới giờ thi, chưa thể phát đề!");
        }
        if (!TrackProblemStatementStorage.hasProblemFile(track)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Bảng đấu chưa có file PDF đề bài — upload trước khi phát");
        }
        track.setProblemReleasedAt(now);
        Track saved = trackRepository.save(track);
        auditService.log(AuditAction.TRACK_RELEASE_PROBLEM, "tracks", trackId, Map.of(
                "trackName", saved.getName(),
                "roundId", round.getId()));
        notifyTrackProblemReleased(saved, round);
        return trackMapper.toResponse(saved);
    }

    private void notifyTrackProblemReleased(Track track, Round round) {
        Set<User> teamMembers = new LinkedHashSet<>();
        for (TeamRoundTrack trt : teamRoundTrackRepository.findByTrack_Round_Id(round.getId())) {
            if (!trt.getTrack().getId().equals(track.getId())) {
                continue;
            }
            teamMemberRepository.findByTeam_Id(trt.getTeam().getId()).stream()
                    .filter(tm -> tm.getStatus() == TeamMemberStatus.ACCEPTED)
                    .map(TeamMember::getUser)
                    .forEach(teamMembers::add);
        }
        if (!teamMembers.isEmpty()) {
            notificationService.sendBatch(
                    new ArrayList<>(teamMembers),
                    "PROBLEM_RELEASED",
                    "Đề Sơ loại — %s".formatted(track.getName()),
                    "Đề bài cho bảng \"%s\" đã được phát. Vào trang đội để tải PDF — mỗi đội chỉ thấy đề của bảng mình."
                            .formatted(track.getName()),
                    "tracks",
                    track.getId());
        }
    }

    private void guardParentStatus(Hackathon h) {
        archiveGuard.assertNotArchived(h);
        if (h == null || !MUTABLE_PARENT.contains(h.getStatus())) {
            throw new ConflictException(ErrorCode.TRACK_HACKATHON_LOCKED,
                    "Hackathon đang %s — không cho thao tác Track".formatted(
                            h == null ? "null" : h.getStatus()));
        }
    }

    private void validateTopicAfterKickoff(Track track, Hackathon hackathon, String newTopic) {
        if (newTopic == null || hackathon == null) {
            return;
        }
        String trimmed = newTopic.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String current = track.getTopic() == null ? "" : track.getTopic().trim();
        if (trimmed.equals(current)) {
            return;
        }
        if (!eventRepository.existsByHackathonIdAndType(hackathon.getId(), EventType.KICKOFF)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chưa có sự kiện KICKOFF — topic Track chỉ cập nhật sau Khai mạc (bốc thăm)",
                    Map.of("trackId", track.getId(), "hackathonId", hackathon.getId()));
        }
    }

    /**
     * Gán {@code sequence_order} cho bảng đấu song song trong round.
     * Không gửi hoặc gửi số đã tồn tại (FE hay default {@code 1}) → {@code max + 1}.
     */
    private int resolveSequenceOrder(Integer roundId, Integer requested) {
        int next = trackRepository.maxSequenceOrderByRoundId(roundId) + 1;
        if (requested == null) {
            return next;
        }
        if (trackRepository.existsByRoundIdAndSequenceOrder(roundId, requested)) {
            log.debug("sequenceOrder {} đã có trong round {} — gán {}", requested, roundId, next);
            return next;
        }
        return requested;
    }

    private void validateSizes(Integer minTeamSize, Integer maxTeamSize,
                               Integer maxTeamsPerGroup, Integer maxTeams) {
        if (minTeamSize != null && maxTeamSize != null && maxTeamSize < minTeamSize) {
            throw new BusinessRuleException(ErrorCode.TRACK_INVALID_TEAM_SIZE,
                    "maxTeamSize (%d) phải >= minTeamSize (%d)".formatted(maxTeamSize, minTeamSize),
                    Map.of("minTeamSize", minTeamSize, "maxTeamSize", maxTeamSize));
        }
        if (maxTeamsPerGroup != null && maxTeams != null && maxTeamsPerGroup > maxTeams) {
            throw new BusinessRuleException(ErrorCode.TRACK_INVALID_GROUP_CAP,
                    "maxTeamsPerGroup (%d) phải <= maxTeams (%d)"
                            .formatted(maxTeamsPerGroup, maxTeams),
                    Map.of("maxTeamsPerGroup", maxTeamsPerGroup, "maxTeams", maxTeams));
        }
    }
}
