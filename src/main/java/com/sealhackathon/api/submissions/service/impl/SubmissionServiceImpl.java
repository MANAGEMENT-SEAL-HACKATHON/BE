package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.submissions.dto.request.ResubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.service.SubmissionService;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final TeamMemberRepository teamMemberRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    @Override
    public SubmissionResponse submit(SubmitSubmissionRequest req) {
        // TODO: FR-22/33 submit with preliminary/final policy checks.
        return SubmissionResponse.builder().build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> list(Integer teamId, Integer roundId) {
        CurrentUserStub user = currentUserAccessor.currentUser();
        List<Submission> rows = switch (user.getRole()) {
            case COORDINATOR -> listForCoordinator(teamId, roundId);
            case JUDGE -> listForJudge(user.getUserId(), teamId, roundId);
            case STUDENT -> listForStudent(user.getUserId(), teamId, roundId);
            default -> throw forbidden("Role không được xem danh sách bài nộp");
        };
        return rows.stream().map(SubmissionServiceImpl::toResponse).toList();
    }

    @Override
    public SubmissionResponse resubmit(Integer submissionId, ResubmitSubmissionRequest req) {
        // TODO: FR-22 resubmit with deadline and lock validation.
        return SubmissionResponse.builder().id(submissionId).build();
    }

    @Override
    public SubmissionResponse review(Integer submissionId, ReviewSubmissionRequest req) {
        // TODO: FR-25 coordinator review late pending submission.
        return SubmissionResponse.builder().id(submissionId).build();
    }

    private List<Submission> listForCoordinator(Integer teamId, Integer roundId) {
        if (teamId != null && roundId != null) {
            return mergeSubmissions(
                    submissionRepository.findByTeam_IdAndRound_Id(teamId, roundId),
                    submissionRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId));
        }
        if (roundId != null) {
            return mergeSubmissions(
                    submissionRepository.findByRound_Id(roundId),
                    submissionRepository.findByTrack_Round_Id(roundId));
        }
        if (teamId != null) {
            return submissionRepository.findByTeam_Id(teamId);
        }
        return List.of();
    }

    private List<Submission> listForJudge(Integer judgeId, Integer teamId, Integer roundId) {
        if (roundId == null) {
            throw forbidden("Judge phải truyền roundId khi xem danh sách bài nộp");
        }
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId)) {
            throw forbidden("Judge chưa được phân công cho round này");
        }
        return listForCoordinator(teamId, roundId);
    }

    private List<Submission> listForStudent(Integer userId, Integer teamId, Integer roundId) {
        if (teamId == null) {
            throw forbidden("Student phải truyền teamId khi xem danh sách bài nộp");
        }
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndStatus(
                userId, teamId, TeamMemberStatus.ACCEPTED)) {
            throw forbidden("Bạn không thuộc đội này");
        }
        return listForCoordinator(teamId, roundId);
    }

    private static List<Submission> mergeSubmissions(List<Submission> a, List<Submission> b) {
        Map<Integer, Submission> byId = new LinkedHashMap<>();
        for (Submission s : a) {
            byId.put(s.getId(), s);
        }
        for (Submission s : b) {
            byId.putIfAbsent(s.getId(), s);
        }
        return new ArrayList<>(byId.values());
    }

    private static AuthException forbidden(String message) {
        return new AuthException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    private static SubmissionResponse toResponse(Submission s) {
        if (s == null) {
            return SubmissionResponse.builder().build();
        }
        return SubmissionResponse.builder()
                .id(s.getId())
                .teamId(s.getTeam() != null ? s.getTeam().getId() : null)
                .trackId(s.getTrack() != null ? s.getTrack().getId() : null)
                .roundId(s.getRound() != null ? s.getRound().getId() : null)
                .repoUrl(s.getRepoUrl())
                .demoUrl(s.getDemoUrl())
                .reportUrl(s.getReportUrl())
                .slideUrl(s.getSlideUrl())
                .status(s.getStatus())
                .isLate(s.getIsLate())
                .lateReason(s.getLateReason())
                .reviewedBy(s.getReviewedBy() != null ? s.getReviewedBy().getId() : null)
                .reviewedAt(s.getReviewedAt())
                .reviewNote(s.getReviewNote())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
