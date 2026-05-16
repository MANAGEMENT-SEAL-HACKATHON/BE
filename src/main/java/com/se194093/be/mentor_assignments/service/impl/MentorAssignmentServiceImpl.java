package com.se194093.be.mentor_assignments.service.impl;

import com.se194093.be.mentor_assignments.dto.request.CreateMentorAssignmentRequest;
import com.se194093.be.mentor_assignments.dto.response.MentorAssignmentResponse;
import com.se194093.be.mentor_assignments.service.MentorAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skeleton — TODO Dev theo {@code docs/api/mf-01/fr-05-personnel.md} §FR-05b.
 *
 * <p>Inject: MentorAssignmentRepository, JudgeAssignmentRepository (conflict 2 chiều),
 * UserRepository, TrackRepository, HackathonRepository, NotificationRepository,
 * MentorAssignmentMapper, AuditService, CurrentUserAccessor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MentorAssignmentServiceImpl implements MentorAssignmentService {

    @Override
    public CreateResult assign(CreateMentorAssignmentRequest req) {
        // TODO Dev:
        //  1. mentor = userRepo.findById(req.mentorId) or 404
        //     guard role=MENTOR & status=APPROVED → 422 USER_INVALID_ROLE / USER_NOT_APPROVED
        //  2. track = trackRepo.findById(req.trackId) or 404
        //     guard track.hackathon.status IN (DRAFT, ONGOING) → 409 TRACK_HACKATHON_LOCKED
        //  3. if mentorAssignmentRepo.existsByMentorIdAndTrackId(mentor.id, track.id)
        //         → 409 MENTOR_ASSIGN_DUPLICATE
        //  4. Conflict check 2 chiều:
        //     conflictsRounds = judgeAssignmentRepo.findRoundsByJudgeIdAndTrackId(mentor.id, track.id)
        //     if conflictsRounds.isEmpty():
        //         if judgeAssignmentRepo.countByJudgeId(mentor.id) == 0:
        //             audit.log(WARNING_CONFLICT_CHECK_SKIPPED, "mentor_assignments", null,
        //                       Map.of("mentorId", mentor.id, "trackId", track.id,
        //                              "reason", "judge_assignments has no row for this user"))
        //         conflictWarning = Optional.empty()
        //     else:
        //         conflictWarning = Optional.of(Warning.of("MENTOR_JUDGE_CONFLICT",
        //                                       "...", Map.of("conflictRoundIds", conflictsRounds)))
        //  5. save MentorAssignment(mentor, track, assignedBy=currentUser)
        //  6. audit.log(MENTOR_ASSIGNED, "mentor_assignments", saved.id, snapshot)
        //  7. return CreateResult(mapper.toResponse(saved), conflictWarning)
        throw new UnsupportedOperationException("FR-05b POST /mentor-assignments - to be implemented");
    }

    @Override
    public List<MentorAssignmentResponse> listByTrack(Integer trackId) {
        // TODO Dev: repo.findByTrackId(trackId).stream().map(mapper::toResponse).toList()
        throw new UnsupportedOperationException("FR-05b GET /tracks/{id}/mentors - to be implemented");
    }

    @Override
    public List<MentorAssignmentResponse> listByMentor(Integer mentorId) {
        // TODO Dev: repo.findByMentorId(mentorId).stream().map(mapper::toResponse).toList()
        throw new UnsupportedOperationException("FR-05b GET /users/{mentorId}/track-assignments - to be implemented");
    }

    @Override
    public Integer unassign(Integer assignmentId) {
        // TODO Dev:
        //  - findById → 404
        //  - delete
        //  - notify mentor via notifications type=MENTOR_UNASSIGNED
        //  - audit MENTOR_UNASSIGNED
        throw new UnsupportedOperationException("FR-05b DELETE /mentor-assignments/{id} - to be implemented");
    }
}
