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
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.mentor_assignments.entity.MentorAssignment;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamPlaceholderRepository;
import com.sealhackathon.api.tracks.dto.request.CreateTrackRequest;
import com.sealhackathon.api.tracks.dto.request.UpdateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.service.TrackService;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
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

    private final TrackRepository trackRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackMapper trackMapper;
    private final AuditService auditService;
    private final TeamPlaceholderRepository teamRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final NotificationService notificationService;
    private final EventRepository eventRepository;
    private final CriteriaRepository criteriaRepository;

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

        Track entity = trackMapper.toEntity(req, round);
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
        validateTopicAfterKickoff(t, parent, req.getTopic());

        TrackResponse before = trackMapper.toResponse(t);
        TrackStatus oldStatus = t.getStatus();
        if (req.getStatus() == TrackStatus.CANCELLED && oldStatus != TrackStatus.CANCELLED) {
            long activeTeams = teamRepository.countActiveByTrackId(id);
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
            auditService.log(AuditAction.TRACK_TOPIC_UPDATE, "tracks", saved.getId(), Map.of(
                    "oldTopic", before.getTopic(),
                    "newTopic", after.getTopic()));
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
        if (parent != null && !MUTABLE_PARENT.contains(parent.getStatus())) {
            throw new ConflictException(ErrorCode.TRACK_HACKATHON_LOCKED,
                    "Không thể xóa Track khi Hackathon ở status %s".formatted(parent.getStatus()));
        }
        if (t.getStatus() != TrackStatus.CANCELLED) {
            throw new ConflictException(ErrorCode.TRACK_NOT_CANCELLED,
                    "Chỉ xóa Track khi status=CANCELLED",
                    Map.of("trackId", id, "status", t.getStatus()));
        }
        if (criteriaRepository.countByTrackId(id) > 0) {
            throw new ConflictException(ErrorCode.TRACK_HAS_CRITERIA,
                    "Track còn Criteria — không thể xóa",
                    Map.of("trackId", id));
        }
        if (teamRepository.countActiveByTrackId(id) > 0) {
            throw new ConflictException(ErrorCode.TRACK_HAS_TEAMS,
                    "Track còn team đang đăng ký");
        }
        if (t.getRound() != null && Boolean.TRUE.equals(t.getRound().getIsActive())) {
            throw new ConflictException(ErrorCode.TRACK_HAS_ACTIVE_ROUND,
                    "Round cha đang active — không thể xóa Track");
        }

        TrackResponse snapshot = trackMapper.toResponse(t);
        List<MentorAssignment> mentors = mentorAssignmentRepository.findByTrackId(id);
        for (MentorAssignment ma : mentors) {
            notificationService.send(ma.getMentor(), "MENTOR_UNASSIGNED",
                    "Track '%s' đã bị xóa".formatted(t.getName()),
                    "Bạn không còn là Mentor của Track này do Track bị xóa.",
                    "tracks", id);
        }
        trackRepository.delete(t);
        auditService.log(AuditAction.TRACK_DELETE, "tracks", id,
                Map.of("snapshot", snapshot, "mentorCount", mentors.size()));
        return id;
    }

    private void guardParentStatus(Hackathon h) {
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
