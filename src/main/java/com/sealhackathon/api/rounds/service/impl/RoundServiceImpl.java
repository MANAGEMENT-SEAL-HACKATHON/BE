package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundService;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.submissions.repository.SubmissionPlaceholderRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RoundServiceImpl implements RoundService {

    private static final Set<HackathonStatus> MUTABLE_PARENT = EnumSet.of(
            HackathonStatus.DRAFT, HackathonStatus.ONGOING);

    private final RoundRepository roundRepository;
    private final HackathonRepository hackathonRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final RoundMapper roundMapper;
    private final AuditService auditService;
    private final WeightSummaryService weightSummaryService;
    private final SubmissionPlaceholderRepository submissionRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final NotificationService notificationService;

    @Override
    public RoundResponse createByHackathon(Integer hackathonId, CreateRoundRequest req) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        guardHackathonMutable(h);
        validateDeadline(req.getSubmissionOpen(), req.getSubmissionDeadline());
        validateRoundBusinessRules(req);
        validateExamAtRules(hackathonId, req.getIsFinal(), req.getExamAt(),
                req.getSubmissionOpen(), null);

        Round entity = roundMapper.toEntity(req, h);
        Round saved = roundRepository.save(entity);

        RoundResponse response = roundMapper.toResponse(saved);
        auditService.log(AuditAction.ROUND_CREATE, "rounds", saved.getId(),
                Map.of("hackathonId", hackathonId, "snapshot", response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundSummaryResponse> listByHackathon(Integer hackathonId) {
        if (!hackathonRepository.existsById(hackathonId)) {
            throw new ResourceNotFoundException("Hackathon", hackathonId);
        }
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundResponse getById(Integer id) {
        Round r = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round", id));
        return roundMapper.toResponse(r);
    }

    @Override
    public RoundResponse update(Integer id, UpdateRoundRequest req) {
        Round r = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round", id));
        if (r.getHackathon() != null) {
            guardHackathonMutable(r.getHackathon());
        }
        validateDeadline(req.getSubmissionOpen(), req.getSubmissionDeadline());
        if (r.getHackathon() != null) {
            validateExamAtRules(r.getHackathon().getId(), r.getIsFinal(), req.getExamAt(),
                    req.getSubmissionOpen(), r.getId());
        }

        if (Boolean.TRUE.equals(req.getForceLocked())
                && (req.getForceLockReason() == null || req.getForceLockReason().isBlank())) {
            throw new BusinessRuleException(ErrorCode.ROUND_FORCE_LOCK_REASON,
                    "Khi forceLocked=true bắt buộc cung cấp forceLockReason",
                    Map.of("roundId", id));
        }

        boolean prevScoringLocked = Boolean.TRUE.equals(r.getScoringLocked());
        boolean prevForceLocked = Boolean.TRUE.equals(r.getForceLocked());

        RoundResponse before = roundMapper.toResponse(r);
        roundMapper.applyUpdate(r, req);
        Round saved = roundRepository.save(r);
        RoundResponse after = roundMapper.toResponse(saved);

        if (!prevScoringLocked && Boolean.TRUE.equals(saved.getScoringLocked())) {
            auditService.log(AuditAction.ROUND_LOCK, "rounds", saved.getId(),
                    Map.of("by", "stub-coordinator"));
        }
        if (!prevForceLocked && Boolean.TRUE.equals(saved.getForceLocked())) {
            auditService.log(AuditAction.ROUND_FORCE_LOCK, "rounds", saved.getId(),
                    Map.of("reason", saved.getForceLockReason()));
        }
        auditService.logBeforeAfter(AuditAction.ROUND_UPDATE, "rounds", saved.getId(), before, after);
        return after;
    }

    @Override
    public Integer delete(Integer id) {
        Round r = roundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Round", id));
        if (Boolean.TRUE.equals(r.getIsActive())) {
            throw new ConflictException(ErrorCode.ROUND_ANOTHER_ACTIVE,
                    "Round đang active — vui lòng deactivate trước khi xóa",
                    Map.of("roundId", id));
        }
        if (submissionRepository.countByRoundId(id) > 0) {
            throw new ConflictException(ErrorCode.ROUND_HAS_SUBMISSIONS,
                    "Round đã có submission — không thể xóa");
        }
        if (criteriaRepository.countByRoundIdOrTracksInRound(id) > 0) {
            throw new ConflictException(ErrorCode.ROUND_HAS_CRITERIA,
                    "Round đã có Criteria — không thể xóa",
                    Map.of("roundId", id));
        }

        RoundResponse snapshot = roundMapper.toResponse(r);
        List<JudgeAssignment> judges = judgeAssignmentRepository.findByRoundId(id);
        for (JudgeAssignment ja : judges) {
            notificationService.send(ja.getJudge(), "JUDGE_UNASSIGNED",
                    "Round '%s' đã bị xóa".formatted(r.getName()),
                    "Bạn không còn là Judge của Round này do Round bị xóa.",
                    "rounds", id);
        }
        roundRepository.delete(r);
        auditService.log(AuditAction.ROUND_DELETE, "rounds", id,
                Map.of("snapshot", snapshot, "judgeCount", judges.size()));
        return id;
    }

    private RoundSummaryResponse toSummary(Round r) {
        int trackCount = Boolean.TRUE.equals(r.getIsFinal())
                ? 0
                : (int) trackRepository.countByRoundId(r.getId());
        int criteriaCount;
        float total;
        if (Boolean.TRUE.equals(r.getIsFinal())) {
            criteriaCount = (int) criteriaRepository.countNormalByFinalRoundId(r.getId());
            total = weightSummaryService.rawTotalForFinalRound(r.getId()).orElse(0.0).floatValue();
        } else {
            criteriaCount = 0;
            total = 0f;
        }
        return roundMapper.toSummary(r, trackCount, criteriaCount, total);
    }

    private void guardHackathonMutable(Hackathon h) {
        if (!MUTABLE_PARENT.contains(h.getStatus())) {
            throw new BusinessRuleException(ErrorCode.TRACK_HACKATHON_LOCKED,
                    "Hackathon status=%s không cho phép sửa cấu trúc Round"
                            .formatted(h.getStatus()),
                    Map.of("hackathonId", h.getId(), "status", h.getStatus()));
        }
    }

    /**
     * Thứ tự vòng theo {@code examAt}: sơ loại/bán kết trước chung kết; tách khỏi deadline nộp bài.
     *
     * @param excludeRoundId round đang sửa (bỏ qua khi so với final), null khi tạo mới
     */
    private void validateExamAtRules(Integer hackathonId, Boolean isFinal, LocalDateTime examAt,
                                     LocalDateTime submissionOpen, Integer excludeRoundId) {
        if (submissionOpen != null && examAt.isBefore(submissionOpen)) {
            throw new BusinessRuleException(ErrorCode.ROUND_EXAM_BEFORE_SUBMISSION_OPEN,
                    "Ngày thi (%s) phải >= thời điểm mở nộp bài (%s)"
                            .formatted(examAt, submissionOpen),
                    Map.of("examAt", examAt, "submissionOpen", submissionOpen));
        }

        if (Boolean.TRUE.equals(isFinal)) {
            if (roundRepository.countByHackathon_IdAndIsFinalTrue(hackathonId) > 0
                    && (excludeRoundId == null)) {
                throw new ConflictException(ErrorCode.ROUND_DUPLICATE_FINAL,
                        "Hackathon đã có Round Chung kết — mỗi kỳ chỉ 1 vòng final",
                        Map.of("hackathonId", hackathonId));
            }
            if (roundRepository.findPreliminaryLikeByHackathonId(hackathonId).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.ROUND_FINAL_REQUIRES_PRELIM,
                        "Tạo Round Chung kết yêu cầu đã có ít nhất một vòng Sơ loại/Bán kết",
                        Map.of("hackathonId", hackathonId));
            }
            roundRepository.maxExamAtNonFinal(hackathonId).ifPresent(maxPrelimExam -> {
                if (!examAt.isAfter(maxPrelimExam)) {
                    throw new BusinessRuleException(ErrorCode.ROUND_FINAL_EXAM_ORDER,
                            "Round Chung kết: ngày thi phải sau vòng Sơ loại (%s)"
                                    .formatted(maxPrelimExam),
                            Map.of("hackathonId", hackathonId,
                                    "examAt", examAt,
                                    "maxPreliminaryExamAt", maxPrelimExam));
                }
            });
            return;
        }

        roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .filter(fr -> excludeRoundId == null || !fr.getId().equals(excludeRoundId))
                .ifPresent(finalRound -> {
                    if (!examAt.isBefore(finalRound.getExamAt())) {
                        throw new BusinessRuleException(ErrorCode.ROUND_PRELIM_EXAM_ORDER,
                                "Vòng Sơ loại/Bán kết: ngày thi phải trước Chung kết (%s)"
                                        .formatted(finalRound.getExamAt()),
                                Map.of("hackathonId", hackathonId,
                                        "examAt", examAt,
                                        "finalExamAt", finalRound.getExamAt(),
                                        "finalRoundId", finalRound.getId()));
                    }
                });
    }

    private void validateRoundBusinessRules(CreateRoundRequest req) {
        boolean isFinal = Boolean.TRUE.equals(req.getIsFinal());
        if (isFinal) {
            if (req.getTopNAdvance() != null || req.getMinTeamsFinal() != null) {
                throw new BusinessRuleException(ErrorCode.ROUND_DEADLINE_INVALID,
                        "Round Chung kết: topNAdvance và minTeamsFinal phải null",
                        Map.of());
            }
            if (req.getRoundType() != null && req.getRoundType() != RoundType.FINAL) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "is_final=TRUE yêu cầu round_type=FINAL", Map.of());
            }
            if (req.getLateSubmissionPolicy() != null
                    && req.getLateSubmissionPolicy() != LateSubmissionPolicy.HARD_LOCK) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Round Chung kết yêu cầu late_submission_policy=HARD_LOCK", Map.of());
            }
        } else {
            if (req.getRoundType() == RoundType.FINAL) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Round Sơ loại/Bán kết: round_type không được FINAL khi is_final=FALSE",
                        Map.of());
            }
        }
    }

    private void validateDeadline(LocalDateTime open, LocalDateTime deadline) {
        if (deadline == null) {
            return;
        }
        if (open != null && !deadline.isAfter(open)) {
            throw new BusinessRuleException(ErrorCode.ROUND_DEADLINE_INVALID,
                    "submissionDeadline (%s) phải sau submissionOpen (%s)".formatted(deadline, open),
                    Map.of("submissionOpen", open, "submissionDeadline", deadline));
        }
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(ErrorCode.ROUND_DEADLINE_INVALID,
                    "submissionDeadline (%s) phải > hiện tại".formatted(deadline),
                    Map.of("submissionDeadline", deadline, "now", LocalDateTime.now()));
        }
    }
}
