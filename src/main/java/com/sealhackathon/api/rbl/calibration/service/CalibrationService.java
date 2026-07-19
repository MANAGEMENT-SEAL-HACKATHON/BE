package com.sealhackathon.api.rbl.calibration.service;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rbl.calibration.dto.CalibrationDtos.*;
import com.sealhackathon.api.rbl.calibration.entity.CalibrationPrompt;
import com.sealhackathon.api.rbl.calibration.entity.CalibrationScore;
import com.sealhackathon.api.rbl.calibration.repository.CalibrationPromptRepository;
import com.sealhackathon.api.rbl.calibration.repository.CalibrationScoreRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalibrationService {
    private final CalibrationPromptRepository promptRepository;
    private final CalibrationScoreRepository scoreRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final CriteriaRepository criteriaRepository;
    private final UserRepository userRepository;
    private final JudgeAssignmentRepository assignmentRepository;
    private final CurrentUserAccessor currentUserAccessor;

    public PromptView create(CreatePromptRequest request) {
        Round round = roundRepository.findById(request.roundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round", request.roundId()));
        if (!round.getHackathon().getId().equals(request.hackathonId())) {
            throw invalid("Vòng không thuộc hackathon đã chọn");
        }
        Submission sample = request.sampleSubmissionId() == null ? null
                : submissionRepository.findById(request.sampleSubmissionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Submission", request.sampleSubmissionId()));
        if (sample != null && !sample.getRound().getId().equals(round.getId())) {
            throw invalid("Bài mẫu không thuộc vòng đã chọn");
        }
        CalibrationPrompt prompt = CalibrationPrompt.builder()
                .hackathon(hackathonRepository.findById(request.hackathonId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hackathon", request.hackathonId())))
                .round(round)
                .title(request.title().trim())
                .description(request.description())
                .sampleSubmission(sample)
                .build();
        return toView(promptRepository.save(prompt));
    }

    public PromptView close(Integer promptId) {
        CalibrationPrompt prompt = getPrompt(promptId);
        prompt.setStatus(CalibrationPrompt.Status.CLOSED);
        prompt.setClosedAt(LocalDateTime.now());
        return toView(promptRepository.save(prompt));
    }

    @Transactional(readOnly = true)
    public List<PromptView> listByRound(Integer roundId) {
        return promptRepository.findByRoundIdOrderByCreatedAtDesc(roundId).stream()
                .map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<PromptView> listOpenForJudge(Integer roundId) {
        requireAssigned(roundId);
        return promptRepository.findByRoundIdAndStatusOrderByCreatedAtDesc(
                roundId, CalibrationPrompt.Status.OPEN).stream().map(this::toView).toList();
    }

    public DistributionView submit(Integer promptId, SubmitScoresRequest request) {
        CalibrationPrompt prompt = getPrompt(promptId);
        if (prompt.getStatus() != CalibrationPrompt.Status.OPEN) {
            throw invalid("Phiên chấm thử đã đóng");
        }
        Integer judgeId = currentUserAccessor.currentUserId();
        requireAssigned(prompt.getRound().getId());
        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", judgeId));
        Map<Integer, Criteria> allowed = criteriaForRound(prompt.getRound().getId()).stream()
                .collect(Collectors.toMap(Criteria::getId, criterion -> criterion));
        for (ScoreInput input : request.scores()) {
            Criteria criterion = allowed.get(input.criterionId());
            if (criterion == null) throw invalid("Tiêu chí không thuộc vòng chấm thử");
            if (input.scoreValue() > criterion.getMaxScore()) {
                throw invalid("Điểm vượt quá điểm tối đa của tiêu chí");
            }
            CalibrationScore score = scoreRepository
                    .findByPromptIdAndJudgeIdAndCriterionId(promptId, judgeId, input.criterionId())
                    .orElseGet(() -> CalibrationScore.builder()
                            .prompt(prompt).judge(judge).criterion(criterion).build());
            score.setScoreValue(input.scoreValue());
            score.setComment(input.comment());
            score.setScoredAt(LocalDateTime.now());
            scoreRepository.save(score);
        }
        return distribution(promptId, true);
    }

    @Transactional(readOnly = true)
    public DistributionView distribution(Integer promptId, boolean requireJudgeAssignment) {
        CalibrationPrompt prompt = getPrompt(promptId);
        if (requireJudgeAssignment) requireAssigned(prompt.getRound().getId());
        List<CalibrationScore> scores =
                scoreRepository.findByPromptIdOrderByJudgeIdAscCriterionIdAsc(promptId);
        Map<Integer, List<CalibrationScore>> byJudge = scores.stream().collect(
                Collectors.groupingBy(s -> s.getJudge().getId(), TreeMap::new, Collectors.toList()));
        List<AnonymousJudgeScores> judges = new ArrayList<>();
        int position = 1;
        for (List<CalibrationScore> judgeScores : byJudge.values()) {
            judges.add(new AnonymousJudgeScores("Giám khảo " + position++, judgeScores.stream()
                    .map(s -> new CriterionScoreView(
                            s.getCriterion().getId(), s.getCriterion().getName(), s.getScoreValue()))
                    .toList()));
        }
        return new DistributionView(prompt.getId(), prompt.getTitle(), judges);
    }

    private void requireAssigned(Integer roundId) {
        Integer judgeId = currentUserAccessor.currentUserId();
        if (!assignmentRepository.existsByJudgeIdAndRoundScope(judgeId, roundId)) {
            throw invalid("Giám khảo chưa được phân công vào vòng này");
        }
    }

    private CalibrationPrompt getPrompt(Integer promptId) {
        return promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("CalibrationPrompt", promptId));
    }

    private PromptView toView(CalibrationPrompt prompt) {
        List<CriterionView> criteria = criteriaForRound(prompt.getRound().getId()).stream()
                .map(c -> new CriterionView(c.getId(), c.getName(), c.getDescription(),
                        c.getMaxScore(), c.getWeight())).toList();
        return new PromptView(prompt.getId(), prompt.getHackathon().getId(), prompt.getRound().getId(),
                prompt.getTitle(), prompt.getDescription(),
                prompt.getSampleSubmission() == null ? null : prompt.getSampleSubmission().getId(),
                prompt.getStatus().name(), prompt.getCreatedAt(), prompt.getClosedAt(), criteria);
    }

    private List<Criteria> criteriaForRound(Integer roundId) {
        List<Criteria> direct = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId);
        if (!direct.isEmpty()) return direct;
        return criteriaRepository.findAll().stream()
                .filter(c -> c.getTrack() != null && c.getTrack().getRound().getId().equals(roundId))
                .sorted(Comparator.comparing(Criteria::getDisplayOrder).thenComparing(Criteria::getId))
                .toList();
    }

    private BusinessRuleException invalid(String message) {
        return new BusinessRuleException(ErrorCode.INVALID_STATE, message);
    }
}
