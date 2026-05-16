package com.se194093.be.mentor_assignments.service;

import com.se194093.be.common.response.Warning;
import com.se194093.be.mentor_assignments.dto.request.CreateMentorAssignmentRequest;
import com.se194093.be.mentor_assignments.dto.response.MentorAssignmentResponse;

import java.util.List;
import java.util.Optional;

/**
 * FR-05b — Phân công Mentor vào Track.
 *
 * <p>Business rules:
 * <ul>
 *   <li>{@code mentorId}: role=MENTOR &amp; status=APPROVED → 422.</li>
 *   <li>{@code trackId}: tồn tại; hackathon.status IN (DRAFT, ONGOING) → 409 nếu sai.</li>
 *   <li>UNIQUE(mentor_id, track_id) → 409 {@code MENTOR_ASSIGN_DUPLICATE}.</li>
 *   <li>Conflict warning 2 chiều với {@code judge_assignments}: nếu user này đang là Judge của Round
 *       trong cùng Track → trả 201 kèm {@code warnings:[{code:"MENTOR_JUDGE_CONFLICT",...}]}.</li>
 *   <li>Nếu {@code judge_assignments} cho user này EMPTY → ghi audit
 *       {@code WARNING_CONFLICT_CHECK_SKIPPED}; KHÔNG đẩy warning ra response.</li>
 * </ul>
 *
 * <p>Audit: {@code MENTOR_ASSIGNED}, {@code MENTOR_UNASSIGNED}.
 */
public interface MentorAssignmentService {

    record CreateResult(MentorAssignmentResponse assignment, Optional<Warning> conflictWarning) {}

    CreateResult assign(CreateMentorAssignmentRequest req);

    List<MentorAssignmentResponse> listByTrack(Integer trackId);

    List<MentorAssignmentResponse> listByMentor(Integer mentorId);

    Integer unassign(Integer assignmentId);
}
