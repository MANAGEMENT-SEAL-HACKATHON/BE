package com.se194093.be.tracks.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.Warning;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.mentor_assignments.entity.MentorAssignment;
import com.se194093.be.mentor_assignments.repository.MentorAssignmentRepository;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.teams.repository.TeamPlaceholderRepository;
import com.se194093.be.tracks.dto.request.CreateTrackRequest;
import com.se194093.be.tracks.dto.request.UpdateTrackRequest;
import com.se194093.be.tracks.dto.response.TrackResponse;
import com.se194093.be.tracks.dto.response.TrackSummaryResponse;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.mapper.TrackMapper;
import com.se194093.be.tracks.repository.TrackRepository;
import com.se194093.be.tracks.service.TrackService;
import com.se194093.be.tracks.value_object.TrackStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FR-02 Track CRUD impl. Guards parent hackathon status, team count (stub),
 * active round trên track. Audit mọi mutation. Notify mentor khi delete cascade.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TrackServiceImpl implements TrackService {

    private static final Set<HackathonStatus> MUTABLE_PARENT = EnumSet.of(
            HackathonStatus.DRAFT, HackathonStatus.ONGOING);

    private final TrackRepository trackRepository;
    private final HackathonRepository hackathonRepository;
    private final TrackMapper trackMapper;
    private final AuditService auditService;
    private final RoundRepository roundRepository;
    private final TeamPlaceholderRepository teamRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final NotificationService notificationService;

    @Override
    public TrackResponse create(Integer hackathonId, CreateTrackRequest req) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        guardParentStatus(h);
        validateSizes(req.getMinTeamSize(), req.getMaxTeamSize(),
                      req.getMaxTeamsPerGroup(), req.getMaxTeams());

        Track entity = trackMapper.toEntity(req, h);
        Track saved = trackRepository.save(entity);

        TrackResponse response = trackMapper.toResponse(saved);
        auditService.log(AuditAction.TRACK_CREATE, "tracks", saved.getId(),
                Map.of("hackathonId", hackathonId, "snapshot", response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackSummaryResponse> listByHackathon(Integer hackathonId, TrackStatus statusFilter) {
        List<Track> tracks = (statusFilter == null)
                ? trackRepository.findByHackathonIdOrderById(hackathonId)
                : trackRepository.findByHackathonIdAndStatus(hackathonId, statusFilter);
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
        guardParentStatus(t.getHackathon());
        validateSizes(req.getMinTeamSize(), req.getMaxTeamSize(),
                      req.getMaxTeamsPerGroup(), req.getMaxTeams());

        TrackResponse before = trackMapper.toResponse(t);
        TrackStatus oldStatus = t.getStatus();
        trackMapper.applyUpdate(t, req);
        Track saved = trackRepository.save(t);
        TrackResponse after = trackMapper.toResponse(saved);

        List<Warning> warnings = new ArrayList<>();
        if (req.getStatus() == TrackStatus.CANCELLED && oldStatus != TrackStatus.CANCELLED) {
            long activeTeams = teamRepository.countActiveByTrackId(id);
            if (activeTeams > 0) {
                warnings.add(Warning.of("TRACK_CANCELLED_HAS_TEAMS",
                        "Track CANCELLED nhưng còn %d team đang đăng ký".formatted(activeTeams),
                        Map.of("trackId", id, "teamsActive", activeTeams)));
            }
        }

        auditService.logBeforeAfter(AuditAction.TRACK_UPDATE, "tracks", saved.getId(), before, after);
        return new UpdateResult(after, warnings);
    }

    @Override
    public Integer delete(Integer id) {
        Track t = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track", id));
        Hackathon parent = t.getHackathon();
        if (parent != null && !MUTABLE_PARENT.contains(parent.getStatus())) {
            throw new ConflictException(ErrorCode.TRACK_HACKATHON_LOCKED,
                    "Không thể xóa Track khi Hackathon ở status %s".formatted(parent.getStatus()));
        }
        if (teamRepository.countActiveByTrackId(id) > 0) {
            throw new ConflictException(ErrorCode.TRACK_HAS_TEAMS,
                    "Track còn team đang đăng ký");
        }
        if (roundRepository.existsByTrackIdAndIsActiveTrue(id)) {
            throw new ConflictException(ErrorCode.TRACK_HAS_ACTIVE_ROUND,
                    "Track còn round đang active");
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
