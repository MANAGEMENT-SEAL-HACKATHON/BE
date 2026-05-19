package com.se194093.be.judge_assignments.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.Warning;
import com.se194093.be.common.security.CurrentUserAccessor;
import com.se194093.be.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.se194093.be.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import com.se194093.be.judge_assignments.mapper.JudgeAssignmentMapper;
import com.se194093.be.judge_assignments.repository.JudgeAssignmentRepository;
import com.se194093.be.judge_assignments.service.JudgeAssignmentService;
import com.se194093.be.judge_assignments.value_object.JudgeAssignmentType;
import com.se194093.be.mentor_assignments.repository.MentorAssignmentRepository;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.repository.TrackRepository;
import com.se194093.be.tracks.support.TrackRoundRules;
import com.se194093.be.users.entity.User;
import com.se194093.be.users.repository.UserRepository;
import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class JudgeAssignmentServiceImpl implements JudgeAssignmentService {

    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final NotificationService notificationService;

    @Override
    public CreateResult assign(CreateJudgeAssignmentRequest req) {
        User judge = loadApprovedJudge(req.getJudgeId());
        JudgeAssignmentType assignType = req.getAssignmentType() != null
                ? req.getAssignmentType() : JudgeAssignmentType.NORMAL;

        if (req.getTrackId() != null) {
            return assignToTrack(judge, req.getTrackId(), assignType);
        }
        return assignToFinalRound(judge, req.getRoundId(), assignType);
    }

    private CreateResult assignToTrack(User judge, Integer trackId, JudgeAssignmentType assignType) {
        if (assignType == JudgeAssignmentType.FINAL_EXTERNAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "FINAL_EXTERNAL không dùng cho Track Sơ loại",
                    Map.of("trackId", trackId));
        }
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        TrackRoundRules.requirePreliminaryAssignmentTrack(track);

        if (mentorAssignmentRepository.existsByMentorIdAndTrackId(judge.getId(), trackId)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT_SAME_TRACK,
                    "User đang là Mentor Track #%d — không thể phân công Judge cùng Track"
                            .formatted(trackId),
                    Map.of("trackId", trackId, "judgeId", judge.getId()));
        }
        if (judgeAssignmentRepository.existsByJudgeIdAndTrackId(judge.getId(), trackId)) {
            throw new ConflictException(ErrorCode.JUDGE_ASSIGN_DUPLICATE,
                    "Judge #%d đã được phân công Track #%d".formatted(judge.getId(), trackId));
        }

        JudgeAssignment saved = saveAssignment(judge, track, null, assignType);
        JudgeAssignmentResponse response = judgeAssignmentMapper.toResponse(saved);
        auditService.log(AuditAction.JUDGE_ASSIGNED, "judge_assignments", saved.getId(), Map.of(
                "judgeId", judge.getId(), "trackId", trackId, "type", assignType.name()));
        notificationService.send(judge, "JUDGE_ASSIGNED",
                "Bạn được phân công làm Judge Track '%s'".formatted(track.getName()),
                "Track: %s".formatted(track.getName()),
                "tracks", trackId);
        return new CreateResult(response, List.of());
    }

    private CreateResult assignToFinalRound(User judge, Integer roundId, JudgeAssignmentType assignType) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND,
                    "Judge qua round_id chỉ cho Round Chung kết",
                    Map.of("roundId", roundId));
        }
        if (assignType != JudgeAssignmentType.FINAL_EXTERNAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Round Chung kết yêu cầu assignment_type=FINAL_EXTERNAL",
                    Map.of("roundId", roundId));
        }
        throw new BusinessRuleException(ErrorCode.JUDGE_FINAL_AT_PHASE1,
                "Phân công Judge Chung kết chỉ thực hiện ở GĐ4 — không làm ở GĐ1",
                Map.of("roundId", roundId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByTrack(Integer trackId) {
        return judgeAssignmentRepository.findByTrackId(trackId).stream()
                .map(judgeAssignmentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByRound(Integer roundId) {
        log.warn("Deprecated: GET /rounds/{}/judges — dùng GET /tracks/{{trackId}}/judges cho Sơ loại", roundId);
        return judgeAssignmentRepository.findByRoundId(roundId).stream()
                .map(judgeAssignmentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByJudge(Integer judgeId) {
        return judgeAssignmentRepository.findByJudgeId(judgeId).stream()
                .map(judgeAssignmentMapper::toResponse).toList();
    }

    @Override
    public Integer unassign(Integer id) {
        JudgeAssignment ja = judgeAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JudgeAssignment", id));
        User judge = ja.getJudge();
        String label = ja.getTrack() != null ? ja.getTrack().getName()
                : (ja.getRound() != null ? ja.getRound().getName() : "?");
        judgeAssignmentRepository.delete(ja);
        notificationService.send(judge, "JUDGE_UNASSIGNED",
                "Bạn không còn phân công Judge '%s'".formatted(label),
                "Unassigned by coordinator.",
                "judge_assignments", id);
        auditService.log(AuditAction.JUDGE_UNASSIGNED, "judge_assignments", id,
                Map.of("judgeId", judge.getId()));
        return id;
    }

    private User loadApprovedJudge(Integer judgeId) {
        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("User (judge)", judgeId));
        if (judge.getRole() != UserRole.JUDGE) {
            throw new BusinessRuleException(ErrorCode.USER_INVALID_ROLE,
                    "User #%d không có role JUDGE".formatted(judge.getId()),
                    Map.of("userId", judge.getId(), "role", judge.getRole()));
        }
        if (judge.getStatus() != UserStatus.APPROVED) {
            throw new BusinessRuleException(ErrorCode.USER_NOT_APPROVED,
                    "User #%d chưa APPROVED".formatted(judge.getId()),
                    Map.of("userId", judge.getId(), "status", judge.getStatus()));
        }
        return judge;
    }

    private JudgeAssignment saveAssignment(User judge, Track track, Round round,
                                           JudgeAssignmentType type) {
        Integer uid = currentUserAccessor.currentUserId();
        return judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .round(round)
                .assignmentType(type)
                .assignedAt(LocalDateTime.now())
                .assignedBy(uid == null ? null : User.builder().id(uid).build())
                .build());
    }
}
