package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.submissions.dto.request.ReviewLateSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.dto.response.SubmissionSlideDownload;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.service.SubmissionMetadataService;
import com.sealhackathon.api.submissions.service.SubmissionService;
import com.sealhackathon.api.submissions.support.GitHubRepoValidator;
import com.sealhackathon.api.submissions.support.SubmissionSlideStorage;
import com.sealhackathon.api.storage.StoredObject;
import com.sealhackathon.api.submissions.value_object.LateReviewDecision;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.support.PrelimMutationGuard;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    private final GitHubRepoValidator gitHubRepoValidator;
    private final SubmissionSlideStorage submissionSlideStorage;
    private final NotificationService notificationService;
    private final PrelimMutationGuard prelimMutationGuard;
    private final RoundPresentationReadiness presentationReadiness;
    private final com.sealhackathon.api.live_scoring.SubmissionRosterPublisher submissionRosterPublisher;

    @Override
    public SubmissionResponse submitMultipart(
            Integer teamId, Integer trackId, Integer roundId,
            String repoUrl, String demoUrl, String lateReason,
            MultipartFile slideFile) {

        SubmitSubmissionRequest req = SubmitSubmissionRequest.builder()
                .teamId(teamId)
                .trackId(trackId)
                .roundId(roundId)
                .repoUrl(repoUrl)
                .demoUrl(demoUrl)
                .lateReason(lateReason)
                .build();
        return submitInternal(req, slideFile, true);
    }

    private SubmissionResponse submitInternal(
            SubmitSubmissionRequest req, MultipartFile slideFile, boolean multipartMode) {
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
        if (team.getHackathon().getStatus() == HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.EVENT_FINISHED, "Hackathon đã kết thúc — không còn nhận bài");
        }
        if (team.getHackathon().getStatus() == HackathonStatus.PENDING_CONFIRM) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_CLOSED,
                    "Hackathon đã đóng sổ chấm — không còn nhận bài");
        }
        if (team.getHackathon().getStatus() == HackathonStatus.DRAFT) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_NOT_STARTED,
                    "Sự kiện chưa mở — chưa đến thời gian nộp bài");
        }
        if (team.getHackathon().getStatus() != HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Hackathon chưa ONGOING");
        }

        // LOGIC PHÂN LUỒNG SƠ LOẠI vs CHUNG KẾT
        var routing = validateSubmissionRouting(req.getTrackId(), req.getRoundId());
        Round round = routing.round();
        Track track = routing.track();

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
            // Sơ loại: chỉ PARTICIPATING được nộp / sửa bài (ADVANCED|ELIMINATED → 403)
            prelimMutationGuard.assertPrelimMutable(teamTrack);
        } else {
            boolean isParticipating = teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), round.getId()).isPresent();
            if (!isParticipating) {
                throw new BusinessRuleException(ErrorCode.TEAM_NOT_IN_ROUND, "Đội không có tên trong danh sách tham gia Vòng Chung kết");
            }
        }

        if (multipartMode) {
            gitHubRepoValidator.validatePublicGitHubRepo(req.getRepoUrl());
        } else {
            validateRepoUrl(req.getRepoUrl());
            validateSlideUrl(req.getSlideUrl());
        }

        LocalDateTime now = LocalDateTime.now();
        // Closed-early ⇒ mọi lần nộp sau đó đều late (tránh race isAfter(deadline==now) = false)
        boolean afterDeadline = round.getSubmissionClosedEarlyAt() != null
                || (round.getSubmissionDeadline() != null && !now.isBefore(round.getSubmissionDeadline()));

        if (afterDeadline && isPresentationShuffled(round, track)) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_LOCKED_AFTER_SHUFFLE,
                    "Đã quay số thuyết trình — không cho nộp bài muộn");
        }

        if (afterDeadline
                && round.getLateSubmissionPolicy() != LateSubmissionPolicy.HARD_LOCK
                && !StringUtils.hasText(req.getLateReason())) {
            throw new BusinessRuleException(ErrorCode.LATE_REASON_REQUIRED,
                    "Bắt buộc nhập lý do khi nộp/sửa bài sau hạn chót");
        }

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
        if (!multipartMode) {
            submission.setReportUrl(req.getReportUrl());
            submission.setSlideUrl(req.getSlideUrl());
        }
        submission.setStatus(status);
        submission.setIsLate(afterDeadline && status != SubmissionStatus.SUBMITTED);
        submission.setLateReason(afterDeadline ? req.getLateReason() : null);
        submission.setSubmittedAt(now);

        if (multipartMode) {
            boolean hadSlideBefore = !isCreate && StringUtils.hasText(submission.getSlideStorageKey());
            boolean slideRequired = isCreate || !hadSlideBefore;
            submissionSlideStorage.validatePdf(slideFile, slideRequired);
        }

        Submission saved = submissionRepository.save(submission);

        if (multipartMode && slideFile != null && !slideFile.isEmpty()) {
            submissionSlideStorage.storeSlide(saved, slideFile);
            saved = submissionRepository.save(saved);
        }

        if (multipartMode && !StringUtils.hasText(saved.getSlideStorageKey())) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FILE,
                    "Không lưu được file slide — vui lòng chọn file PDF và nộp lại");
        }

        auditService.log(isCreate ? AuditAction.SUBMISSION_CREATE : AuditAction.SUBMISSION_UPDATE,
                "submissions", saved.getId(),
                Map.of("teamId", team.getId(), "roundId", round.getId()));
        notifySubmissionReceived(team, round, track, saved, isCreate);
        submissionMetadataService.enqueueFetch(saved.getId());
        submissionRosterPublisher.publishInvalidate(round.getId());
        return toResponse(saved, false);
    }

    private void notifySubmissionReceived(Team team, Round round, Track track,
                                          Submission saved, boolean isCreate) {
        List<User> members = teamMemberRepository.findByTeam_Id(team.getId()).stream()
                .filter(tm -> tm.getStatus() == TeamMemberStatus.ACCEPTED)
                .map(TeamMember::getUser)
                .filter(u -> u != null)
                .toList();
        if (members.isEmpty()) {
            return;
        }
        String scope = track != null ? track.getName() : round.getName();
        String statusNote = switch (saved.getStatus()) {
            case SUBMITTED -> "Bài đã được ghi nhận đúng hạn.";
            case LATE_PENDING -> "Bài nộp trễ đang chờ Coordinator duyệt.";
            case LATE_APPROVED -> "Bài nộp trễ đã được duyệt.";
            case ACCEPTED -> "Bài đã được chấp nhận.";
            default -> "Bài đã được ghi nhận.";
        };
        notificationService.sendBatch(
                members,
                "SUBMISSION_RECEIVED",
                "%s bài — %s".formatted(isCreate ? "Đã nộp" : "Đã cập nhật", scope),
                "Đội \"%s\" %s %s".formatted(
                        team.getTeamName(),
                        isCreate ? "vừa nộp bài cho" : "vừa cập nhật bài nộp cho",
                        "\"%s\". %s".formatted(scope, statusNote)),
                "submissions",
                saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionSlideDownload getSlideDownload(Integer submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        assertSlideAccess(submission);
        String filename = SubmissionSlideStorage.displayFilename(submission);
        return new SubmissionSlideDownload(
                submissionSlideStorage.loadSlide(submission),
                filename != null ? filename : "slide.pdf");
    }

    private void assertSlideAccess(Submission submission) {
        CurrentUserStub user = currentUserAccessor.currentUser();
        switch (user.getRole()) {
            case COORDINATOR -> { /* ok */ }
            case JUDGE -> {
                Integer roundId = submission.getRound().getId();
                if (!judgeAssignmentRepository.existsByJudgeIdAndRoundScope(user.getUserId(), roundId)) {
                    throw forbidden("Judge chưa được phân công cho round này");
                }
            }
            case STUDENT -> {
                if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndStatus(
                        user.getUserId(), submission.getTeam().getId(), TeamMemberStatus.ACCEPTED)) {
                    throw forbidden("Bạn không thuộc đội này");
                }
            }
            default -> throw forbidden("Không có quyền xem slide");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> list(Integer teamId, Integer roundId, SubmissionStatus status) {
        CurrentUserStub user = currentUserAccessor.currentUser();
        List<Submission> rows = switch (user.getRole()) {
            case COORDINATOR -> listForCoordinator(teamId, roundId, status);
            case JUDGE, MENTOR -> listForJudge(user.getUserId(), teamId, roundId, status);
            case STUDENT -> listForStudent(user.getUserId(), teamId, roundId, status);
            default -> throw forbidden("Role không được xem danh sách bài nộp");
        };
        boolean anonymous = user.getRole() == UserRole.JUDGE;
        return rows.stream().map(s -> toResponse(s, anonymous)).toList();
    }

    @Override
    public SubmissionResponse reviewLate(Integer submissionId, ReviewLateSubmissionRequest req) {
        Submission submission = submissionRepository.findByIdForUpdate(submissionId)
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

        if (isPresentationShuffled(round, submission.getTrack())) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_LOCKED_AFTER_SHUFFLE,
                    "Đã quay số thuyết trình — không cho duyệt bài nộp muộn");
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
        // Không append queue sau shuffle — đã chặn ở trên.
        if (saved.getRound() != null) {
            submissionRosterPublisher.publishInvalidate(saved.getRound().getId());
        }
        return toResponse(saved, false);
    }

    private boolean isPresentationShuffled(Round round, Track track) {
        if (round == null) {
            return false;
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return Boolean.TRUE.equals(round.getPresentationShuffled());
        }
        if (track != null) {
            return Boolean.TRUE.equals(track.getPresentationShuffled());
        }
        return presentationReadiness.isShuffled(round);
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

    private record SubmissionRouting(Round round, Track track) {}

    private SubmissionRouting validateSubmissionRouting(Integer trackId, Integer roundId) {
        if (trackId != null) {
            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
            Round round = track.getRound();
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Vòng Chung kết bắt buộc phải gửi roundId (không dùng trackId)");
            }
            return new SubmissionRouting(round, track);
        }
        if (roundId != null) {
            Round round = roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
            if (!Boolean.TRUE.equals(round.getIsFinal())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Vòng Sơ loại bắt buộc phải gửi kèm trackId");
            }
            return new SubmissionRouting(round, null);
        }
        throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Phải cung cấp trackId hoặc roundId");
    }

    private SubmissionStatus resolveStatusOnSubmit(Submission existing, Round round, boolean afterDeadline) {
        SubmissionStatus computed = resolveSubmitStatus(round, afterDeadline);
        if (existing == null || existing.getStatus() == null) {
            return computed;
        }
        return switch (existing.getStatus()) {
            case LATE_APPROVED, ACCEPTED -> existing.getStatus();
            // Mọi chỉnh sau deadline/close-early → LATE_PENDING/REJECTED (không giữ SUBMITTED)
            case SUBMITTED -> afterDeadline ? computed : existing.getStatus();
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

    private void validateSlideUrl(String slideUrl) {
        if (slideUrl != null && slideUrl.trim().toLowerCase().endsWith(".pdf")) {
            throw new BusinessRuleException(ErrorCode.INVALID_SLIDE_FORMAT,
                    "slideUrl không chấp nhận file PDF — dùng Google Slides hoặc link khác");
        }
    }

    private List<Submission> listForCoordinator(Integer teamId, Integer roundId, SubmissionStatus status) {
        if (status != null && teamId == null && roundId == null) {
            return submissionRepository.findByStatus(status);
        }
        if (status != null && roundId != null && teamId == null) {
            return submissionRepository.findByStatusAndRound_Id(status, roundId);
        }
        List<Submission> rows;
        if (teamId != null && roundId != null) {
            rows = mergeSubmissions(
                    submissionRepository.findByTeam_IdAndRound_Id(teamId, roundId),
                    submissionRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId));
        } else if (roundId != null) {
            rows = mergeSubmissions(
                    submissionRepository.findByRound_Id(roundId),
                    submissionRepository.findByTrack_Round_Id(roundId));
        } else if (teamId != null) {
            rows = submissionRepository.findByTeam_Id(teamId);
        } else {
            rows = List.of();
        }
        if (status == null) {
            return rows;
        }
        return rows.stream().filter(s -> s.getStatus() == status).toList();
    }

    private List<Submission> listForJudge(Integer judgeId, Integer teamId, Integer roundId, SubmissionStatus status) {
        if (roundId == null) {
            throw forbidden("Judge phải truyền roundId khi xem danh sách bài nộp");
        }
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId)) {
            throw forbidden("Judge chưa được phân công cho round này");
        }
        return listForCoordinator(teamId, roundId, status);
    }

    private List<Submission> listForStudent(Integer userId, Integer teamId, Integer roundId, SubmissionStatus status) {
        if (teamId == null) {
            throw forbidden("Student phải truyền teamId khi xem danh sách bài nộp");
        }
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndStatus(
                userId, teamId, TeamMemberStatus.ACCEPTED)) {
            throw forbidden("Bạn không thuộc đội này");
        }
        return listForCoordinator(teamId, roundId, status);
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

    static SubmissionResponse toResponse(Submission s, boolean anonymous) {
        if (s == null) {
            return SubmissionResponse.builder().build();
        }
        String slideFile = SubmissionSlideStorage.displayFilename(s);
        String slideDownloadPath = null;
        if (slideFile != null && !anonymous && s.getId() != null) {
            slideDownloadPath = "/api/v1/submissions/" + s.getId() + "/slide";
        }

        return SubmissionResponse.builder()
                .id(s.getId())
                .displayCode(s.getId() != null ? "#" + s.getId() : null)
                .teamId(anonymous ? null : (s.getTeam() != null ? s.getTeam().getId() : null))
                .teamName(anonymous ? null : (s.getTeam() != null ? s.getTeam().getTeamName() : null))
                .trackId(s.getTrack() != null ? s.getTrack().getId() : null)
                .roundId(s.getRound() != null ? s.getRound().getId() : null)
                .repoUrl(s.getRepoUrl())
                .demoUrl(s.getDemoUrl())
                .slideFile(slideFile)
                .slideDownloadPath(slideDownloadPath)
                .status(s.getStatus())
                .isLate(s.getIsLate())
                .lateReason(anonymous ? null : s.getLateReason())
                .reviewedBy(anonymous ? null : (s.getReviewedBy() != null ? s.getReviewedBy().getId() : null))
                .reviewedAt(anonymous ? null : s.getReviewedAt())
                .reviewNote(anonymous ? null : s.getReviewNote())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}