package com.se194093.be.rounds.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import com.se194093.be.judge_assignments.repository.JudgeAssignmentRepository;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.dto.request.UpdateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.dto.response.RoundSummaryResponse;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.mapper.RoundMapper;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.rounds.service.RoundService;
import com.se194093.be.submissions.repository.SubmissionPlaceholderRepository;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FR-03 Round CRUD. KHÔNG validate weight ở POST/PUT — chỉ validate ở activate (FR-06B).
 * Audit transitions {@code scoringLocked} / {@code forceLocked} riêng (ROUND_LOCK / ROUND_FORCE_LOCK).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RoundServiceImpl implements RoundService {

    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final RoundMapper roundMapper;
    private final AuditService auditService;
    private final WeightSummaryService weightSummaryService;
    private final SubmissionPlaceholderRepository submissionRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final NotificationService notificationService;

    @Override
    public RoundResponse create(Integer trackId, CreateRoundRequest req) {
        Track t = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        validateDeadline(req.getSubmissionOpen(), req.getSubmissionDeadline());

        Round entity = roundMapper.toEntity(req, t);
        Round saved = roundRepository.save(entity);

        RoundResponse response = roundMapper.toResponse(saved);
        auditService.log(AuditAction.ROUND_CREATE, "rounds", saved.getId(),
                Map.of("trackId", trackId, "snapshot", response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundSummaryResponse> listByTrack(Integer trackId) {
        return roundRepository.findByTrackIdOrderBySequenceOrderAsc(trackId).stream()
                .map(r -> {
                    int criteriaCount = (int) criteriaRepository.countNormalByRoundId(r.getId());
                    float total = weightSummaryService.rawTotal(r.getId()).orElse(0.0).floatValue();
                    return roundMapper.toSummary(r, criteriaCount, total);
                })
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
        validateDeadline(req.getSubmissionOpen(), req.getSubmissionDeadline());

        if (Boolean.TRUE.equals(req.getForceLocked())
                && (req.getForceLockReason() == null || req.getForceLockReason().isBlank())) {
            throw new BusinessRuleException(ErrorCode.ROUND_FORCE_LOCK_REASON,
                    "Khi forceLocked=true bắt buộc cung cấp forceLockReason",
                    Map.of("roundId", id));
        }

        boolean prevScoringLocked = Boolean.TRUE.equals(r.getScoringLocked());
        boolean prevForceLocked   = Boolean.TRUE.equals(r.getForceLocked());

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
