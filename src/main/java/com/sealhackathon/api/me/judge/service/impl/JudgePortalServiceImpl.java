package com.sealhackathon.api.me.judge.service.impl;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.exception.ScoringLockedException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoreCommentRequest;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoringCompletionRequest;
import com.sealhackathon.api.me.judge.dto.request.TiebreakVoteRequest;
import com.sealhackathon.api.me.judge.dto.response.*;
import com.sealhackathon.api.me.judge.service.JudgePortalService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
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

    @Override
    public List<JudgeTrackAssignmentResponse> listTrackAssignments() {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy tất cả phân công của Giám khảo
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);

        // Lọc ra các phân công thuộc Vòng Sơ Loại (Có Track)
        return assignments.stream()
                .filter(ja -> ja.getTrack() != null)
                .map(ja -> JudgeTrackAssignmentResponse.builder()
                        .assignmentId(ja.getId())
                        .trackId(ja.getTrack().getId())
                        .trackName(ja.getTrack().getName())
                        .roundId(ja.getTrack().getRound().getId())
                        .assignmentType(ja.getAssignmentType().name())
                        .completionStatus(ja.getCompletionStatus().name())
                        .build())
                .toList();
    }

    @Override
    public List<JudgeFinalAssignmentResponse> listFinalAssignments() {
        Integer judgeId = currentUserAccessor.currentUserId();

        // Lấy tất cả phân công của Giám khảo
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByJudgeId(judgeId);

        // Lọc ra các phân công thuộc Vòng Chung Kết (Không có Track, gắn trực tiếp vào Round)
        return assignments.stream()
                .filter(ja -> ja.getRound() != null && Boolean.TRUE.equals(ja.getRound().getIsFinal()))
                .map(ja -> JudgeFinalAssignmentResponse.builder()
                        .assignmentId(ja.getId())
                        .hackathonId(ja.getRound().getHackathon().getId())
                        .role(ja.getAssignmentType().name()) // Trả về FINAL_EXTERNAL
                        .build())
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
                        .teamId(score.getSubmission().getTeam().getId())
                        .totalScore(BigDecimal.valueOf(score.getScoreValue()))
                        .comment(score.getComment())
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
                .teamId(score.getSubmission().getTeam().getId())
                .totalScore(BigDecimal.valueOf(score.getScoreValue()))
                .comment(score.getComment())
                .build();
    }

    @Override
    @Transactional
    public TiebreakVoteResponse submitTiebreakVote(TiebreakVoteRequest request) {
        Integer judgeId = currentUserAccessor.currentUserId();
        Integer roundId = request.getRoundId();

        // 1. Kiểm tra Quyền HEAD Judge
        boolean isHead = judgeAssignmentRepository.findByJudgeId(judgeId).stream()
                .anyMatch(ja -> ja.getAssignmentType() == JudgeAssignmentType.HEAD
                        && ((ja.getRound() != null && ja.getRound().getId().equals(roundId))
                        || (ja.getTrack() != null && ja.getTrack().getRound().getId().equals(roundId))));

        if (!isHead) {
            throw new AuthException(ErrorCode.FORBIDDEN,
                    "Chỉ Trưởng nhóm Giám khảo (HEAD) mới có quyền quyết định Tiebreak", HttpStatus.FORBIDDEN);
        }

        // 2. Lấy thông tin Round
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new ScoringLockedException("Vòng thi đã đóng sổ, không thể thay đổi Tiebreak");
        }

        // 3. Xóa các vote cũ của HEAD Judge này trong Round hiện tại
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
                    .tiebreakLevel(2) // Level 2: HEAD Vote
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
}