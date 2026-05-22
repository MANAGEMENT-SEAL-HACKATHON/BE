package com.sealhackathon.api.judge_assignments.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.sealhackathon.api.judge_assignments.dto.response.JudgeAssignmentResponse;

import java.util.List;

/**
 * FR-05c — Phân công Judge sơ bộ vào Round.
 *
 * <p>Business rules:
 * <ul>
 *   <li>{@code judgeId}: role=JUDGE &amp; status=APPROVED → 422.</li>
 *   <li>{@code roundId}: tồn tại → 404.</li>
 *   <li>UNIQUE(judge_id, round_id) → 409 {@code JUDGE_ASSIGN_DUPLICATE}.</li>
 * </ul>
 *
 * <p>Warnings mềm (không block):
 * <ul>
 *   <li>{@code JUDGE_FINAL_ROUND_AT_PHASE1}: nếu gán Judge cho Round Chung kết ({@code isFinal}).</li>
 *   <li>{@code MENTOR_JUDGE_CONFLICT}: nếu user đang là Mentor của Track chứa Round này.</li>
 *   <li>{@code CONFLICT_CHECK_SKIPPED}: nếu {@code mentor_assignments} cho user này empty (audit).</li>
 * </ul>
 *
 * <p>Audit: {@code JUDGE_ASSIGNED}, {@code JUDGE_UNASSIGNED}.
 */
public interface JudgeAssignmentService {

    record CreateResult(JudgeAssignmentResponse assignment, List<Warning> warnings) {}

    CreateResult assign(CreateJudgeAssignmentRequest req);

    List<JudgeAssignmentResponse> listByTrack(Integer trackId);

    List<JudgeAssignmentResponse> listByRound(Integer roundId);

    List<JudgeAssignmentResponse> listByJudge(Integer judgeId);

    Integer unassign(Integer assignmentId);
}
