package com.sealhackathon.api.mentor_assignments.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.mentor_assignments.dto.request.CreateMentorAssignmentRequest;
import com.sealhackathon.api.mentor_assignments.dto.response.MentorAssignmentResponse;

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
 *   <li>Track {@code status=OPEN} (không CANCELLED) → 422 {@code INVALID_STATE}.</li>
 *   <li>Conflict Mentor↔Judge cùng track → 422 {@code CONFLICT_SAME_TRACK}.</li>
 *   <li>Đã Judge Chung kết cùng hackathon → 422 {@code FINAL_JUDGE_CANNOT_BE_MENTOR}.</li>
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
