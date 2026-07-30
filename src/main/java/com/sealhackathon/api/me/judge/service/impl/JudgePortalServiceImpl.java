package com.sealhackathon.api.me.judge.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.exception.ScoringLockedException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.value_object.AssignmentResponseStatus;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoreCommentRequest;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoringCompletionRequest;
import com.sealhackathon.api.me.judge.dto.request.TiebreakVoteRequest;
import com.sealhackathon.api.me.judge.dto.response.*;
import com.sealhackathon.api.me.judge.service.JudgePortalService;
import com.sealhackathon.api.events.entity.JudgeSubmissionScoringConfirmation;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.support.SubmissionSlideStorage;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.presentation.support.PresentationScoringCompletionHelper;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JudgePortalServiceImpl implements JudgePortalService {

    private final CurrentUserAccessor currentUserAccessor;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final ScoreRepository scoreRepository;
    private final TiebreakEvaluationRepository tiebreakEvaluationRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final JudgeAssignmentGuard judgeAssignmentGuard;
    private final CriteriaRepository criteriaRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;
    private final PresentationScoringCompletionHelper scoringCompletionHelper;
    private final PresentationControllerGuard presentationControllerGuard;
    private final TrackRepository trackRepository;

    @Override
    public List<JudgeTrackAssignmentResponse> listTrackAssignments() {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy tất cả phân công của Giám khảo
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);

        // Lọc ra các phân công thuộc Vòng Sơ Loại (Có Track)
        return assignments.stream()
                .filter(ja -> ja.getTrack() != null)
                .map(ja -> {
                    Round round = ja.getTrack().getRound();
                    var hackathon = round.getHackathon();
                    Integer trackId = ja.getTrack().getId();
                    int totalTeams = countGradableSubmissionsForTrack(trackId);
                    int scoredTeams = countFullyScoredSubmissionsForJudge(judgeId, trackId);
                    return JudgeTrackAssignmentResponse.builder()
                            .assignmentId(ja.getId())
                            .hackathonId(hackathon.getId())
                            .hackathonName(hackathon.getName())
                            .trackId(trackId)
                            .trackName(ja.getTrack().getName())
                            .roundId(round.getId())
                            .roundName(round.getName())
                            .assignmentType(ja.getAssignmentType().name())
                            .completionStatus(ja.getCompletionStatus().name())
                            .totalTeams(totalTeams)
                            .scoredTeams(scoredTeams)
                            .responseStatus(ja.getResponseStatus() != null
                                    ? ja.getResponseStatus().name()
                                    : AssignmentResponseStatus.ACCEPTED.name())
                            .declineReason(ja.getDeclineReason())
                            .build();
                })
                .toList();
    }

    private int countGradableSubmissionsForTrack(Integer trackId) {
        return (int) submissionRepository.findByTrack_Id(trackId).stream()
                .filter(SubmissionGradablePolicy::isGradable)
                .count();
    }

    private int countFullyScoredSubmissionsForJudge(Integer judgeId, Integer trackId) {
        return (int) submissionRepository.findByTrack_Id(trackId).stream()
                .filter(SubmissionGradablePolicy::isGradable)
                .filter(submission -> scoringCompletionHelper.hasJudgeFullyScored(judgeId, submission))
                .count();
    }

    @Override
    public List<JudgeFinalAssignmentResponse> listFinalAssignments() {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy tất cả phân công của Giám khảo
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);

        // Lọc ra các phân công thuộc Vòng Chung Kết (Không có Track, gắn trực tiếp vào Round)
        return assignments.stream()
                .filter(ja -> ja.getRound() != null && Boolean.TRUE.equals(ja.getRound().getIsFinal()))
                .map(ja -> {
                    Round round = ja.getRound();
                    var hackathon = round.getHackathon();
                    return JudgeFinalAssignmentResponse.builder()
                            .assignmentId(ja.getId())
                            .hackathonId(hackathon.getId())
                            .hackathonName(hackathon.getName())
                            .roundId(round.getId())
                            .roundName(round.getName())
                            .role(ja.getAssignmentType().name()) // Trả về FINAL_EXTERNAL
                            .responseStatus(ja.getResponseStatus() != null
                                    ? ja.getResponseStatus().name()
                                    : AssignmentResponseStatus.ACCEPTED.name())
                            .declineReason(ja.getDeclineReason())
                            .build();
                })
                .toList();
    }

    @Override
    public List<JudgeScoreSummaryResponse> listMyScores(Integer roundId) {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy điểm CHÍNH MÌNH đã chấm (RBL Anonymization - BR-J-17), loại bỏ điểm PENALTY / CALIBRATION
        List<Score> myScores = scoreRepository.findMyScores(judgeId, ScoreType.NORMAL, roundId);

        return myScores.stream()
                .map(score -> JudgeScoreSummaryResponse.builder()
                        .scoreId(score.getId())
                        .submissionId(score.getSubmission().getId())
                        .criterionId(score.getCriterion().getId())
                        .displayCode("#" + score.getSubmission().getId())
                        .totalScore(BigDecimal.valueOf(score.getScoreValue()))
                        .comment(score.getComment())
                        .build())
                .toList();
    }

    @Override
    public List<JudgeSubmissionListItemResponse> listSubmissions(Integer roundId, Integer trackId) {
        Integer judgeId = currentUserAccessor.currentUserId();
        if (roundId == null) {
            throw new AuthException(ErrorCode.VALIDATION_FAILED, "roundId bắt buộc", HttpStatus.BAD_REQUEST);
        }
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId)) {
            throw new AuthException(ErrorCode.FORBIDDEN, "Judge chưa được phân công round này", HttpStatus.FORBIDDEN);
        }

        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);
        var assignedTrackIds = assignments.stream()
                .filter(ja -> ja.getTrack() != null && ja.getTrack().getRound().getId().equals(roundId))
                .map(ja -> ja.getTrack().getId())
                .collect(java.util.stream.Collectors.toSet());

        boolean finalRound = assignments.stream()
                .anyMatch(ja -> ja.getRound() != null && ja.getRound().getId().equals(roundId));

        List<Submission> submissions = submissionRepository.findByTrack_Round_Id(roundId);
        if (submissions.isEmpty()) {
            submissions = submissionRepository.findByRound_Id(roundId);
        }

        return submissions.stream()
                .filter(SubmissionGradablePolicy::isGradable)
                .filter(s -> {
                    if (trackId != null) {
                        return s.getTrack() != null && s.getTrack().getId().equals(trackId);
                    }
                    if (s.getTrack() != null) {
                        return assignedTrackIds.contains(s.getTrack().getId());
                    }
                    return finalRound;
                })
                .map(s -> JudgeSubmissionListItemResponse.builder()
                        .submissionId(s.getId())
                        .displayCode("#" + s.getId())
                        .trackId(s.getTrack() != null ? s.getTrack().getId() : null)
                        .trackName(s.getTrack() != null ? s.getTrack().getName() : "Chung kết")
                        .status(s.getStatus())
                        .slideFile(SubmissionSlideStorage.displayFilename(s))
                        .repoUrl(s.getRepoUrl())
                        .build())
                .toList();
    }

    @Override
    public List<JudgeScoringScheduleItemResponse> getScoringSchedule(Integer roundId) {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy tất cả phân công của Giám khảo
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);

        return assignments.stream()
                .map(ja -> ja.getRound() != null ? ja.getRound() : ja.getTrack().getRound()) // Lấy Round từ phân công
                .filter(round -> roundId == null || round.getId().equals(roundId)) // Lọc theo roundId nếu có
                .distinct() // Tránh trùng lặp nếu Giám khảo chấm nhiều Track trong cùng 1 Round
                .map(round -> JudgeScoringScheduleItemResponse.builder()
                        .roundId(round.getId())
                        .roundName(round.getName())
                        // Thời gian bắt đầu chấm điểm = Hạn chót nộp bài
                        .scoringStartAt(round.getSubmissionDeadline())
                        // Thời gian kết thúc chấm điểm = Thời điểm khóa sổ (nếu có)
                        .scoringEndAt(Boolean.TRUE.equals(round.getScoringLocked()) ? LocalDateTime.now() : null)
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void updateScoringCompletion(JudgeScoringCompletionRequest request) {
        Integer judgeId = currentUserAccessor.currentUserId();

        // 1. Tìm bản ghi Phân công (Assignment)
        JudgeAssignment assignment = judgeAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new com.sealhackathon.api.common.exception.ResourceNotFoundException("JudgeAssignment", request.getAssignmentId()));

        // 2. RÀO CHẮN: Giám khảo chỉ được phép cập nhật tiến độ CỦA CHÍNH MÌNH
        if (!assignment.getJudge().getId().equals(judgeId)) {
            throw new com.sealhackathon.api.common.exception.AuthException(
                    com.sealhackathon.api.common.exception.ErrorCode.FORBIDDEN,
                    "Bạn không có quyền cập nhật tiến độ chấm thi của Giám khảo khác",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }

        // 3. RÀO CHẮN: Vòng thi đóng sổ thì không được cập nhật nữa
        com.sealhackathon.api.rounds.entity.Round round = assignment.getRound() != null
                ? assignment.getRound() : assignment.getTrack().getRound();

        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new com.sealhackathon.api.common.exception.ScoringLockedException("Vòng thi đã đóng sổ, không thể cập nhật tiến độ");
        }

        // 4. Parse Enum và Lưu dữ liệu
        com.sealhackathon.api.judge_assignments.value_object.CompletionStatus newStatus;
        try {
            newStatus = com.sealhackathon.api.judge_assignments.value_object.CompletionStatus.valueOf(request.getCompletionStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.sealhackathon.api.common.exception.BusinessRuleException(
                    com.sealhackathon.api.common.exception.ErrorCode.VALIDATION_FAILED,
                    "Trạng thái completion_status không hợp lệ (Phải là NOT_STARTED, IN_PROGRESS, hoặc COMPLETED)");
        }

        assignment.setCompletionStatus(newStatus);
        assignment.setCompletionUpdatedAt(LocalDateTime.now());
        judgeAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public JudgeScoreSummaryResponse updateScoreComment(Integer scoreId, JudgeScoreCommentRequest request) {
        Integer judgeId = currentUserAccessor.currentUserId();

        // 1. Tìm điểm và kiểm tra quyền sở hữu
        Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Score", scoreId));

        if (!score.getJudge().getId().equals(judgeId)) {
            throw new AuthException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền sửa lời phê của Giám khảo khác", HttpStatus.FORBIDDEN);
        }

        // 2. Rào chắn: Kiểm tra Vòng thi đã khóa chưa
        Round round = score.getSubmission().getRound();
        if (round == null && score.getSubmission().getTrack() != null) {
            round = score.getSubmission().getTrack().getRound();
        }
        if (round != null && Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new ScoringLockedException("Vòng thi đã đóng sổ, không thể sửa lời phê");
        }

        // 3. Cập nhật comment
        score.setComment(request.getComment());
        score.setUpdatedAt(LocalDateTime.now());
        scoreRepository.save(score);

        return JudgeScoreSummaryResponse.builder()
                .scoreId(score.getId())
                .submissionId(score.getSubmission().getId())
                .displayCode("#" + score.getSubmission().getId())
                .totalScore(BigDecimal.valueOf(score.getScoreValue()))
                .comment(score.getComment())
                .build();
    }

    @Override
    @Transactional
    public TiebreakVoteResponse submitTiebreakVote(TiebreakVoteRequest request) {
        Integer judgeId = currentUserAccessor.currentUserId();
        Integer roundId = request.getRoundId();

        // 1. Bất kỳ giám khảo được phân công trong phạm vi vòng thi (round CK hoặc track thuộc round)
        //    đều được quyền quyết định Tiebreak — không còn giới hạn HEAD.
        boolean isAssignedJudge = judgeAssignmentRepository.findByJudgeId(judgeId).stream()
                .anyMatch(ja -> (ja.getRound() != null && ja.getRound().getId().equals(roundId))
                        || (ja.getTrack() != null && ja.getTrack().getRound().getId().equals(roundId)));

        if (!isAssignedJudge) {
            throw new AuthException(ErrorCode.FORBIDDEN,
                    "Bạn chưa được phân công Giám khảo trong vòng thi này — không có quyền quyết định Tiebreak",
                    HttpStatus.FORBIDDEN);
        }

        // 2. Lấy thông tin Round
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new ScoringLockedException("Vòng thi đã đóng sổ, không thể thay đổi Tiebreak");
        }

        // 3. Xóa các vote cũ của Giám khảo này trong Round hiện tại
        tiebreakEvaluationRepository.deleteByRound_IdAndJudge_IdAndIsCastingVoteTrue(roundId, judgeId);

        // 4. Lưu thứ tự các đội được Vote (Đội xếp trước được điểm phạt ít hơn)
        List<Integer> teamIds = request.getOrderedTeamIds();
        for (int i = 0; i < teamIds.size(); i++) {
            Integer teamId = teamIds.get(i);
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

            TiebreakEvaluation evaluation = TiebreakEvaluation.builder()
                    .round(round)
                    .team(team)
                    .judge(User.builder().id(judgeId).build())
                    .penaltyScore((float) i) // Đội xếp 0 -> Penalty 0; Xếp 1 -> Penalty 1
                    .isCastingVote(true)
                    .tiebreakLevel(2) // Level 2: Casting Vote của giám khảo
                    .evaluatedAt(LocalDateTime.now())
                    .build();

            tiebreakEvaluationRepository.save(evaluation);
        }

        return TiebreakVoteResponse.builder()
                .roundId(roundId)
                .orderedTeamIds(teamIds)
                .status("SUBMITTED")
                .build();
    }

    @Override
    public JudgeHistoryResponse getHistory(Integer year) {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy tất cả phân công của Giám khảo
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);

        List<JudgeHistoryResponse.JudgeHistoryItem> items = assignments.stream()
                .map(ja -> ja.getRound() != null ? ja.getRound() : ja.getTrack().getRound())
                // RÀO CHẮN: Hackathon phải ĐÃ KẾT THÚC (FINISHED) mới đưa vào lịch sử
                .filter(round -> round.getHackathon().getStatus() == com.sealhackathon.api.hackathons.value_object.HackathonStatus.FINISHED)
                // Lọc theo năm nếu Giám khảo có truyền param
                .filter(round -> year == null || round.getHackathon().getYear().equals(year))
                .distinct()
                .map(round -> JudgeHistoryResponse.JudgeHistoryItem.builder()
                        .hackathonId(round.getHackathon().getId())
                        .hackathonName(round.getHackathon().getName())
                        .roundId(round.getId())
                        .roundName(round.getName())
                        .build())
                .toList();

        return JudgeHistoryResponse.builder().items(items).build();
    }

    @Override
    public JudgePresentationScoringStatusResponse getPresentationScoringStatus(Integer roundId, Integer trackId) {
        Integer judgeId = currentUserAccessor.currentUserId();
        if (roundId == null) {
            throw new AuthException(ErrorCode.VALIDATION_FAILED, "roundId bắt buộc", HttpStatus.BAD_REQUEST);
        }
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId)) {
            throw new AuthException(ErrorCode.FORBIDDEN, "Judge chưa được phân công round này", HttpStatus.FORBIDDEN);
        }

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        Track track = trackId != null ? trackRepository.findById(trackId).orElse(null) : null;
        boolean canControl = track != null
                ? presentationControllerGuard.canControlTrack(judgeId, track, round, false)
                : presentationControllerGuard.canControlRound(judgeId, round, false);

        PresentationSlot presenting = resolvePresentingSlot(roundId, trackId);
        if (presenting == null || presenting.getSubmission() == null) {
            return JudgePresentationScoringStatusResponse.builder()
                    .roundId(roundId)
                    .trackId(trackId)
                    .judgesAssigned(scoringCompletionHelper.countAssignedJudges(trackId, roundId))
                    .canControlPresentation(canControl)
                    .build();
        }

        Submission submission = presenting.getSubmission();
        int judgesAssigned = scoringCompletionHelper.countAssignedJudges(trackId, roundId);
        int judgesScored = scoringCompletionHelper.countDistinctJudgesWithAnyScore(submission.getId());
        int judgesFullyScored = scoringCompletionHelper.countJudgesFullyScored(submission);
        int judgesConfirmed = scoringCompletionHelper.countJudgesConfirmed(submission.getId());
        boolean myScored = scoringCompletionHelper.hasJudgeFullyScored(judgeId, submission);
        boolean myConfirmed = scoringConfirmationRepository.existsBySubmission_IdAndJudge_Id(
                submission.getId(), judgeId);
        boolean scoringComplete = scoringCompletionHelper.canAdvanceQueue(submission, trackId, roundId);
        boolean naturalPartialOk = Boolean.FALSE.equals(presenting.getQaEndedEarly()) && judgesScored > 0;
        boolean canAdvance = scoringComplete || naturalPartialOk;
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            PresentationTimerPhase phase = presenting.getTimerPhase();
            boolean timerReady = phase == PresentationTimerPhase.QA || phase == PresentationTimerPhase.ENDED;
            canAdvance = canAdvance && timerReady;
        }

        LocalDateTime lastJudgeScoredAt = scoreRepository.findBySubmission_Id(submission.getId()).stream()
                .map(s -> s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getScoredAt())
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return JudgePresentationScoringStatusResponse.builder()
                .roundId(roundId)
                .trackId(trackId)
                .submissionId(submission.getId())
                .displayCode("#" + submission.getId())
                .judgesAssigned(judgesAssigned)
                .judgesScored(judgesScored)
                .judgesFullyScored(judgesFullyScored)
                .judgesConfirmed(judgesConfirmed)
                .myConfirmed(myConfirmed)
                .myScored(myScored)
                .allJudgesSubmitted(scoringComplete)
                .canAdvanceQueue(canAdvance)
                .qaEndedEarly(presenting.getQaEndedEarly())
                .canControlPresentation(canControl)
                .lastJudgeScoredAt(lastJudgeScoredAt)
                .build();
    }

    @Override
    @Transactional
    public void confirmSubmissionScoring(Integer submissionId) {
        Integer judgeId = currentUserAccessor.currentUserId();
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        judgeAssignmentGuard.requireJudgeForSubmission(judgeId, submission);

        Round round = submission.getRound() != null
                ? submission.getRound()
                : submission.getTrack().getRound();
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new ScoringLockedException("Vòng thi đã đóng sổ");
        }

        if (!scoringCompletionHelper.hasJudgeFullyScored(judgeId, submission)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Cần chấm đủ tất cả tiêu chí trước khi xác nhận chấm xong");
        }

        PresentationSlot slot = presentationSlotRepository
                .findByRound_IdAndSubmission_Id(round.getId(), submissionId)
                .orElse(null);
        if (slot == null || slot.getQueueStatus() != PresentationQueueStatus.PRESENTING) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ xác nhận chấm xong khi bài đang thuyết trình (PRESENTING)");
        }

        if (scoringConfirmationRepository.existsBySubmission_IdAndJudge_Id(submissionId, judgeId)) {
            return;
        }

        User judge = User.builder().id(judgeId).build();
        scoringConfirmationRepository.save(JudgeSubmissionScoringConfirmation.builder()
                .submission(submission)
                .judge(judge)
                .confirmedAt(LocalDateTime.now())
                .build());
    }

    private PresentationSlot resolvePresentingSlot(Integer roundId, Integer trackId) {
        List<PresentationSlot> slots = trackId != null
                ? presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(roundId, trackId)
                : presentationSlotRepository.findByRound_IdOrderBySequenceOrderAsc(roundId);
        return slots.stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.PRESENTING)
                .findFirst()
                .orElse(null);
    }

}