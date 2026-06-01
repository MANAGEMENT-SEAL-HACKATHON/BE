package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.submissions.dto.request.ResubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewLateSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.service.SubmissionMetadataService;
import com.sealhackathon.api.submissions.service.SubmissionService;
import com.sealhackathon.api.submissions.value_object.LateReviewDecision;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final RoundAccessGuard roundAccessGuard;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SubmissionMetadataService submissionMetadataService;
    private final RoundRepository roundRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;

    @Override
    public SubmissionResponse submit(SubmitSubmissionRequest req) {
        CurrentUserStub actor = currentUserAccessor.currentUser();
        if (actor.getRole() != UserRole.STUDENT || actor.getStatus() != UserStatus.APPROVED) {
            throw forbidden("Chỉ sinh viên đã duyệt mới được nộp bài");
        }
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndStatus(
                actor.getUserId(), req.getTeamId(), TeamMemberStatus.ACCEPTED)) {
            throw new BusinessRuleException(ErrorCode.NOT_TEAM_MEMBER, "Bạn không thuộc đội này");
        }

        Team team = teamRepository.findById(req.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", req.getTeamId()));
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new BusinessRuleException(ErrorCode.TEAM_NOT_ACTIVE, "Đội chưa ACTIVE");
        }
        if (team.getHackathon().getStatus() != HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Hackathon chưa ONGOING");
        }

        // LOGIC PHÂN LUỒNG SƠ LOẠI vs CHUNG KẾT
        Round round;
        Track track = null;

        if (req.getTrackId() != null) {
            track = trackRepository.findById(req.getTrackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Track", req.getTrackId()));
            round = track.getRound();
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Vòng Chung kết không sử dụng Track, vui lòng bỏ trống trackId");
            }
        } else if (req.getRoundId() != null) {
            round = roundRepository.findById(req.getRoundId())
                    .orElseThrow(() -> new ResourceNotFoundException("Round", req.getRoundId()));
            if (!Boolean.TRUE.equals(round.getIsFinal())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Vòng Sơ loại bắt buộc phải gửi kèm trackId");
            }
        } else {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Phải cung cấp trackId hoặc roundId");
        }

        if (!team.getHackathon().getId().equals(round.getHackathon().getId())) {
            throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION, "Round/Track không thuộc hackathon của đội");
        }

        roundAccessGuard.requireActiveRound(round.getId());

        // Validate Quyền Tham Gia (Sơ loại dùng TeamRoundTrack, Chung kết dùng TeamRoundParticipation)
        if (track != null) {
            Integer currentTrackId = track.getId();

            var teamTrack = teamRoundTrackRepository.findByTeam_IdAndTrack_Id(team.getId(), currentTrackId)
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.TEAM_NOT_IN_TRACK,
                            "Đội chưa được gán track này",
                            Map.of("teamId", team.getId(), "trackId", currentTrackId)));
            if (teamTrack.getParticipationStatus() == ParticipationStatus.ELIMINATED) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Đội đã bị loại khỏi vòng này");
            }
        } else {
            boolean isParticipating = teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), round.getId()).isPresent();
            if (!isParticipating) {
                throw new BusinessRuleException(ErrorCode.TEAM_NOT_IN_ROUND, "Đội không có tên trong danh sách tham gia Vòng Chung kết");
            }
        }

        validateRepoUrl(req.getRepoUrl());

        LocalDateTime now = LocalDateTime.now();
        boolean afterDeadline = now.isAfter(round.getSubmissionDeadline());

        Submission submission;
        if (track != null) {
            submission = submissionRepository.findTopByTeam_IdAndTrack_IdOrderBySubmittedAtDesc(team.getId(), track.getId()).orElse(null);
        } else {
            submission = submissionRepository.findTopByTeam_IdAndRound_IdOrderBySubmittedAtDesc(team.getId(), round.getId()).orElse(null);
        }

        boolean isCreate = submission == null;
        if (submission != null && submission.getStatus() == SubmissionStatus.REJECTED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Bài nộp đã bị từ chối — không thể nộp lại");
        }
        if (submission == null) {
            submission = Submission.builder()
                    .team(team)
                    .hackathon(team.getHackathon())
                    .track(track)
                    .round(round)
                    .build();
        }

        SubmissionStatus status = resolveStatusOnSubmit(submission, round, afterDeadline);

        submission.setRepoUrl(req.getRepoUrl());
        submission.setDemoUrl(req.getDemoUrl());
        submission.setReportUrl(req.getReportUrl());
        submission.setSlideUrl(req.getSlideUrl());
        submission.setStatus(status);
        submission.setIsLate(afterDeadline && status != SubmissionStatus.SUBMITTED);
        submission.setLateReason(afterDeadline ? req.getLateReason() : null);
        submission.setSubmittedAt(now);

        Submission saved = submissionRepository.save(submission);
        auditService.log(isCreate ? AuditAction.SUBMISSION_CREATE : AuditAction.SUBMISSION_UPDATE,
                "submissions", saved.getId(),
                Map.of("teamId", team.getId(), "roundId", round.getId()));
        submissionMetadataService.enqueueFetch(saved.getId());
        return toResponse(saved);
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
        Submission existing = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        SubmitSubmissionRequest upsert = SubmitSubmissionRequest.builder()
                .teamId(existing.getTeam().getId())
                .trackId(existing.getTrack() != null ? existing.getTrack().getId() : null)
                .roundId(existing.getRound().getId())
                .repoUrl(req.getRepoUrl() != null ? req.getRepoUrl() : existing.getRepoUrl())
                .demoUrl(req.getDemoUrl() != null ? req.getDemoUrl() : existing.getDemoUrl())
                .reportUrl(req.getReportUrl() != null ? req.getReportUrl() : existing.getReportUrl())
                .slideUrl(req.getSlideUrl() != null ? req.getSlideUrl() : existing.getSlideUrl())
                .lateReason(req.getReason() != null ? req.getReason() : existing.getLateReason())
                .build();
        return submit(upsert);
    }

    @Override
    public SubmissionResponse reviewLate(Integer submissionId, ReviewLateSubmissionRequest req) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        if (submission.getStatus() != SubmissionStatus.LATE_PENDING) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_NOT_LATE_PENDING,
                    "Bài nộp không ở trạng thái LATE_PENDING");
        }

        Round round = submission.getRound();
        if (Boolean.TRUE.equals(round.getIsFinal())
                && round.getLateSubmissionPolicy() == LateSubmissionPolicy.HARD_LOCK) {
            throw new BusinessRuleException(ErrorCode.LATE_PENDING_NOT_ALLOWED,
                    "Round Chung kết không cho duyệt bài trễ");
        }

        if (req.getDecision() == LateReviewDecision.REJECT
                && !StringUtils.hasText(req.getNote())) {
            throw new BusinessRuleException(ErrorCode.REVIEW_NOTE_REQUIRED,
                    "Bắt buộc ghi chú khi từ chối bài nộp trễ");
        }

        User reviewer = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));

        submission.setStatus(req.getDecision() == LateReviewDecision.APPROVE
                ? SubmissionStatus.LATE_APPROVED
                : SubmissionStatus.REJECTED);
        submission.setReviewedBy(reviewer);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setReviewNote(req.getNote());

        Submission saved = submissionRepository.save(submission);
        auditService.log(AuditAction.SUBMISSION_LATE_REVIEW, "submissions", saved.getId(),
                Map.of("decision", req.getDecision().name()));
        return toResponse(saved);
    }

    @Override
    @Deprecated
    public SubmissionResponse review(Integer submissionId, ReviewSubmissionRequest req) {
        LateReviewDecision decision = req.getStatus() == SubmissionStatus.REJECTED
                ? LateReviewDecision.REJECT
                : LateReviewDecision.APPROVE;
        return reviewLate(submissionId, ReviewLateSubmissionRequest.builder()
                .decision(decision)
                .note(req.getReviewNote())
                .build());
    }

    private SubmissionStatus resolveSubmitStatus(Round round, boolean afterDeadline) {
        if (!afterDeadline) {
            return SubmissionStatus.SUBMITTED;
        }
        if (round.getLateSubmissionPolicy() == LateSubmissionPolicy.HARD_LOCK) {
            return SubmissionStatus.REJECTED;
        }
        return SubmissionStatus.LATE_PENDING;
    }

    private SubmissionStatus resolveStatusOnSubmit(Submission existing, Round round, boolean afterDeadline) {
        SubmissionStatus computed = resolveSubmitStatus(round, afterDeadline);
        if (existing == null || existing.getStatus() == null) {
            return computed;
        }
        return switch (existing.getStatus()) {
            case LATE_APPROVED, ACCEPTED -> existing.getStatus();
            case SUBMITTED -> afterDeadline ? computed : SubmissionStatus.SUBMITTED;
            case LATE_PENDING -> computed;
            default -> computed;
        };
    }

    private void validateRepoUrl(String repoUrl) {
        if (repoUrl != null && repoUrl.toLowerCase().contains("drive.google.com")) {
            throw new BusinessRuleException(ErrorCode.INVALID_REPO_PLATFORM,
                    "Không chấp nhận Google Drive làm repo — dùng GitHub/GitLab");
        }
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

    static SubmissionResponse toResponse(Submission s) {
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