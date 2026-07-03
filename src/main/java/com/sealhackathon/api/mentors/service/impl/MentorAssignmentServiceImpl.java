package com.sealhackathon.api.mentors.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.FrontendUrls;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.dto.request.CreateMentorAssignmentRequest;
import com.sealhackathon.api.mentors.dto.response.MentorAssignmentResponse;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.mapper.MentorAssignmentMapper;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.service.MentorAssignmentService;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.support.TrackRoundRules;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.support.PersonnelAssignmentRules;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * FR-05b Mentor assignment — user MENTOR hoặc JUDGE; cấm Mentor+Judge cùng track (§14 cross-track OK).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MentorAssignmentServiceImpl implements MentorAssignmentService {

    private static final Set<HackathonStatus> MUTABLE_PARENT = EnumSet.of(
            HackathonStatus.DRAFT, HackathonStatus.ONGOING);

    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final MentorAssignmentMapper mentorAssignmentMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final NotificationService notificationService;
    private final HackathonArchiveGuard archiveGuard;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Override
    public CreateResult assign(CreateMentorAssignmentRequest req) {
        User mentor = userRepository.findById(req.getMentorId())
                .orElseThrow(() -> new ResourceNotFoundException("User (mentor)", req.getMentorId()));
        PersonnelAssignmentRules.requireApprovedPersonnel(mentor, "Mentor");

        Track track = trackRepository.findById(req.getTrackId())
                .orElseThrow(() -> new ResourceNotFoundException("Track", req.getTrackId()));
        TrackRoundRules.requirePreliminaryAssignmentTrack(track);
        if (track.getStatus() == TrackStatus.CANCELLED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track #%d đã CANCELLED — không phân công Mentor".formatted(track.getId()),
                    Map.of("trackId", track.getId(), "status", track.getStatus()));
        }
        if (track.getStatus() != TrackStatus.OPEN) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track #%d phải OPEN để phân công Mentor (hiện %s)"
                            .formatted(track.getId(), track.getStatus()),
                    Map.of("trackId", track.getId(), "status", track.getStatus()));
        }

        Hackathon parent = track.getHackathon();
        archiveGuard.assertNotArchived(parent);
        if (parent != null && !MUTABLE_PARENT.contains(parent.getStatus())) {
            throw new ConflictException(ErrorCode.TRACK_HACKATHON_LOCKED,
                    "Hackathon đang %s — không cho phân công Mentor".formatted(parent.getStatus()));
        }

        if (mentorAssignmentRepository.existsByMentorIdAndTrackId(mentor.getId(), track.getId())) {
            throw new ConflictException(ErrorCode.MENTOR_ASSIGN_DUPLICATE,
                    "Mentor #%d đã được phân công vào Track #%d"
                            .formatted(mentor.getId(), track.getId()));
        }

        validateJudgeConflicts(mentor.getId(), track.getId());

        Integer uid = currentUserAccessor.currentUserId();
        MentorAssignment entity = MentorAssignment.builder()
                .mentor(mentor)
                .track(track)
                .assignedAt(LocalDateTime.now())
                .assignedBy(uid == null ? null : User.builder().id(uid).build())
                .build();
        MentorAssignment saved = mentorAssignmentRepository.save(entity);
        MentorAssignmentResponse response = mentorAssignmentMapper.toResponse(saved);

        auditService.log(AuditAction.MENTOR_ASSIGNED, "mentor_assignments", saved.getId(), Map.of(
                "mentorId", mentor.getId(),
                "trackId", track.getId(),
                "snapshot", response
        ));

        notificationService.send(mentor, "MENTOR_ASSIGNED",
                "Bạn được phân công làm Mentor Track '%s'".formatted(track.getName()),
                "Hackathon: %s | Chúc bạn hỗ trợ teams tốt!".formatted(
                        parent == null ? "?" : parent.getName()),
                "tracks", track.getId());

        try {
            emailService.sendMentorAssignment(mentor.getEmail(), mentor.getFullName(),
                    track.getName(), parent == null ? null : parent.getName(),
                    FrontendUrls.loginUrl(appProperties));
        } catch (RuntimeException ex) {
            log.warn("[MentorAssign] email failed for {}: {}", mentor.getEmail(), ex.getMessage());
        }

        return new CreateResult(response, Optional.empty());
    }

    private void validateJudgeConflicts(Integer mentorId, Integer trackId) {
        if (judgeAssignmentRepository.existsByJudgeIdAndTrackId(mentorId, trackId)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT_SAME_TRACK,
                    "User đang là Judge Track #%d — không thể phân công Mentor cùng Track"
                            .formatted(trackId),
                    Map.of("trackId", trackId, "mentorId", mentorId));
        }
        if (judgeAssignmentRepository.existsFinalExternalJudgeInHackathonOfTrack(
                mentorId, trackId, JudgeAssignmentType.FINAL_EXTERNAL)) {
            throw new BusinessRuleException(ErrorCode.FINAL_JUDGE_CANNOT_BE_MENTOR,
                    "User đã là Judge Chung kết — không thể làm Mentor Sơ loại",
                    Map.of("trackId", trackId, "mentorId", mentorId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorAssignmentResponse> listByTrack(Integer trackId) {
        return mentorAssignmentRepository.findByTrackId(trackId).stream()
                .map(mentorAssignmentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorAssignmentResponse> listByMentor(Integer mentorId) {
        return mentorAssignmentRepository.findByMentorId(mentorId).stream()
                .map(mentorAssignmentMapper::toResponse).toList();
    }

    @Override
    public Integer unassign(Integer assignmentId) {
        MentorAssignment ma = mentorAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorAssignment", assignmentId));
        if (ma.getTrack() != null) {
            archiveGuard.assertNotArchivedForTrack(ma.getTrack());
        }
        MentorAssignmentResponse snapshot = mentorAssignmentMapper.toResponse(ma);
        User mentor = ma.getMentor();
        Track track = ma.getTrack();

        mentorAssignmentRepository.delete(ma);

        notificationService.send(mentor, "MENTOR_UNASSIGNED",
                "Bạn không còn là Mentor Track '%s'".formatted(track == null ? "?" : track.getName()),
                "Phân công đã được hủy bởi Coordinator.",
                "tracks", track == null ? null : track.getId());

        auditService.log(AuditAction.MENTOR_UNASSIGNED, "mentor_assignments", assignmentId,
                Map.of("snapshot", snapshot));
        return assignmentId;
    }
}
