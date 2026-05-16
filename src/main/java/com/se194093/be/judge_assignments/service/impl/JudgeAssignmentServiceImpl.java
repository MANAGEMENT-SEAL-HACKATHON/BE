package com.se194093.be.judge_assignments.service.impl;

import com.se194093.be.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.se194093.be.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.se194093.be.judge_assignments.service.JudgeAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skeleton — TODO Dev theo {@code docs/api/mf-01/fr-05-personnel.md} §FR-05c.
 *
 * <p>Inject: JudgeAssignmentRepository, MentorAssignmentRepository (conflict 2 chiều),
 * UserRepository, RoundRepository, NotificationRepository, JudgeAssignmentMapper,
 * AuditService, CurrentUserAccessor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JudgeAssignmentServiceImpl implements JudgeAssignmentService {

    @Override
    public CreateResult assign(CreateJudgeAssignmentRequest req) {
        // TODO Dev:
        //  1. judge = userRepo.findById(req.judgeId) or 404
        //     guard role=JUDGE & status=APPROVED → 422
        //  2. round = roundRepo.findById(req.roundId) or 404
        //  3. if existsByJudgeIdAndRoundId(judge.id, round.id) → 409 JUDGE_ASSIGN_DUPLICATE
        //  4. assignmentType = req.assignmentType != null ? req.assignmentType : NORMAL
        //  5. save JudgeAssignment(judge, round, assignmentType, assignedBy=currentUser)
        //  6. warnings = []
        //  7. Final-round warn: maxSeq = roundRepo.findMaxSequenceByTrackId(round.track.id)
        //     if maxSeq != null && round.sequenceOrder.equals(maxSeq):
        //         warnings.add(Warning.of("JUDGE_FINAL_ROUND_AT_PHASE1",
        //             "Round Chung kết — khuyến nghị phân công ở GĐ5 (FR-27)",
        //             Map.of("roundId", round.id, "trackId", round.track.id)))
        //  8. Conflict 2 chiều với mentor_assignments:
        //     mentorConflicts = mentorAssignmentRepo.findByMentorIdAndTrackId(judge.id, round.track.id)
        //     if mentorConflicts.isEmpty():
        //         if mentorAssignmentRepo.countByMentorId(judge.id) == 0:
        //             audit.log(WARNING_CONFLICT_CHECK_SKIPPED, "judge_assignments", null,
        //                       Map.of("judgeId", judge.id, "roundId", round.id,
        //                              "reason", "mentor_assignments empty"))
        //     else:
        //         warnings.add(Warning.of("MENTOR_JUDGE_CONFLICT",
        //             "User đang là Mentor của Track #...",
        //             Map.of("trackId", round.track.id, "mentorAssignmentIds", mentorConflicts...)))
        //  9. audit.log(JUDGE_ASSIGNED, "judge_assignments", saved.id,
        //               Map.of("judgeId", judge.id, "roundId", round.id, "type", assignmentType))
        // 10. return CreateResult(mapper.toResponse(saved), warnings)
        throw new UnsupportedOperationException("FR-05c POST /judge-assignments - to be implemented");
    }

    @Override
    public List<JudgeAssignmentResponse> listByRound(Integer roundId) {
        // TODO Dev: repo.findByRoundId(roundId).stream().map(mapper::toResponse).toList()
        throw new UnsupportedOperationException("FR-05c GET /rounds/{id}/judges - to be implemented");
    }

    @Override
    public List<JudgeAssignmentResponse> listByJudge(Integer judgeId) {
        // TODO Dev: repo.findByJudgeId(judgeId).stream().map(mapper::toResponse).toList()
        throw new UnsupportedOperationException("FR-05c GET /users/{judgeId}/round-assignments - to be implemented");
    }

    @Override
    public Integer unassign(Integer assignmentId) {
        // TODO Dev:
        //  - findById → 404
        //  - delete; audit JUDGE_UNASSIGNED; notify judge
        throw new UnsupportedOperationException("FR-05c DELETE /judge-assignments/{id} - to be implemented");
    }
}
