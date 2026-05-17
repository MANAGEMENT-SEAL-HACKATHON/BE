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
import com.se194093.be.mentor_assignments.entity.MentorAssignment;
import com.se194093.be.mentor_assignments.repository.MentorAssignmentRepository;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.users.entity.User;
import com.se194093.be.users.repository.UserRepository;
import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FR-05c Judge assignment impl. Warning JUDGE_FINAL_ROUND_AT_PHASE1 + MENTOR_JUDGE_CONFLICT 2 chiều.
 * Notify judge khi unassign.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class JudgeAssignmentServiceImpl implements JudgeAssignmentService {

    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final NotificationService notificationService;

    @Override
    public CreateResult assign(CreateJudgeAssignmentRequest req) {
        User judge = userRepository.findById(req.getJudgeId())
                .orElseThrow(() -> new ResourceNotFoundException("User (judge)", req.getJudgeId()));
        if (judge.getRole() != UserRole.JUDGE) {
            throw new BusinessRuleException(ErrorCode.USER_INVALID_ROLE,
                    "User #%d không có role JUDGE (hiện %s)".formatted(judge.getId(), judge.getRole()),
                    Map.of("userId", judge.getId(), "role", judge.getRole()));
        }
        if (judge.getStatus() != UserStatus.APPROVED) {
            throw new BusinessRuleException(ErrorCode.USER_NOT_APPROVED,
                    "User #%d chưa APPROVED (hiện %s)".formatted(judge.getId(), judge.getStatus()),
                    Map.of("userId", judge.getId(), "status", judge.getStatus()));
        }
        Round round = roundRepository.findById(req.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round", req.getRoundId()));

        if (judgeAssignmentRepository.existsByJudgeIdAndRoundId(judge.getId(), round.getId())) {
            throw new ConflictException(ErrorCode.JUDGE_ASSIGN_DUPLICATE,
                    "Judge #%d đã được phân công Round #%d"
                            .formatted(judge.getId(), round.getId()));
        }

        JudgeAssignmentType assignType = (req.getAssignmentType() != null)
                ? req.getAssignmentType() : JudgeAssignmentType.NORMAL;

        Integer uid = currentUserAccessor.currentUserId();
        JudgeAssignment entity = JudgeAssignment.builder()
                .judge(judge)
                .round(round)
                .assignmentType(assignType)
                .assignedAt(LocalDateTime.now())
                .assignedBy(uid == null ? null : User.builder().id(uid).build())
                .build();
        JudgeAssignment saved = judgeAssignmentRepository.save(entity);
        JudgeAssignmentResponse response = judgeAssignmentMapper.toResponse(saved);

        List<Warning> warnings = new ArrayList<>();
        Track track = round.getTrack();
        Integer trackId = track == null ? null : track.getId();
        if (trackId != null) {
            Integer maxSeq = roundRepository.findMaxSequenceByTrackId(trackId);
            if (maxSeq != null && maxSeq.equals(round.getSequenceOrder())) {
                warnings.add(Warning.of("JUDGE_FINAL_ROUND_AT_PHASE1",
                        "Round '%s' là Round Chung kết Track — phân công Judge khuyến nghị làm ở GĐ5"
                                .formatted(round.getName()),
                        Map.of("roundId", round.getId(), "trackId", trackId,
                               "sequenceOrder", round.getSequenceOrder())));
                auditService.log(AuditAction.WARNING_JUDGE_FINAL_AT_PHASE1, "judge_assignments",
                        saved.getId(),
                        Map.of("roundId", round.getId(), "trackId", trackId));
            }

            List<MentorAssignment> mentorConflicts = mentorAssignmentRepository
                    .findByMentorIdAndTrackId(judge.getId(), trackId);
            if (!mentorConflicts.isEmpty()) {
                List<Integer> ids = mentorConflicts.stream().map(MentorAssignment::getId).toList();
                warnings.add(Warning.of("MENTOR_JUDGE_CONFLICT",
                        "User đang là Mentor của Track #%d — chấm điểm Judge sẽ gây xung đột"
                                .formatted(trackId),
                        Map.of("trackId", trackId, "mentorAssignmentIds", ids)));
            } else if (mentorAssignmentRepository.countByMentorId(judge.getId()) == 0) {
                auditService.log(AuditAction.WARNING_CONFLICT_CHECK_SKIPPED, "judge_assignments",
                        saved.getId(),
                        Map.of("judgeId", judge.getId(), "roundId", round.getId(),
                               "reason", "mentor_assignments empty for this user"));
            }
        }

        auditService.log(AuditAction.JUDGE_ASSIGNED, "judge_assignments", saved.getId(), Map.of(
                "judgeId",   judge.getId(),
                "roundId",   round.getId(),
                "type",      assignType.name(),
                "warningCount", warnings.size()
        ));

        notificationService.send(judge, "JUDGE_ASSIGNED",
                "Bạn được phân công làm Judge Round '%s'".formatted(round.getName()),
                "Track: %s | Chuẩn bị sẵn sàng cho phiên chấm điểm.".formatted(
                        track == null ? "?" : track.getName()),
                "rounds", round.getId());

        return new CreateResult(response, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByRound(Integer roundId) {
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
    public Integer unassign(Integer assignmentId) {
        JudgeAssignment ja = judgeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("JudgeAssignment", assignmentId));
        JudgeAssignmentResponse snapshot = judgeAssignmentMapper.toResponse(ja);
        User judge = ja.getJudge();
        Round round = ja.getRound();

        judgeAssignmentRepository.delete(ja);

        notificationService.send(judge, "JUDGE_UNASSIGNED",
                "Bạn không còn là Judge Round '%s'".formatted(round == null ? "?" : round.getName()),
                "Phân công đã được hủy bởi Coordinator.",
                "rounds", round == null ? null : round.getId());

        auditService.log(AuditAction.JUDGE_UNASSIGNED, "judge_assignments", assignmentId,
                Map.of("snapshot", snapshot));
        return assignmentId;
    }
}
