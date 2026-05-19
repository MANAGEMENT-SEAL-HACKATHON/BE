package com.se194093.be.rounds.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.value_object.HackathonStatus;
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
import com.se194093.be.rounds.value_object.LateSubmissionPolicy;
import com.se194093.be.rounds.value_object.RoundType;
import com.se194093.be.submissions.repository.SubmissionPlaceholderRepository;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.repository.TrackRepository;
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
        validateFinalSequenceOrder(hackathonId, req);

        Round entity = roundMapper.toEntity(req, h);
        Round saved = roundRepository.save(entity);

        RoundResponse response = roundMapper.toResponse(saved);
        auditService.log(AuditAction.ROUND_CREATE, "rounds", saved.getId(),
                Map.of("hackathonId", hackathonId, "snapshot", response));
        return response;
    }

    @Override
    @Deprecated
    public RoundResponse create(Integer trackId, CreateRoundRequest req) {
        log.warn("Deprecated API: POST /tracks/{}/rounds — dùng POST /hackathons/{{id}}/rounds", trackId);
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        if (track.getRound() == null || track.getRound().getHackathon() == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track chưa gắn Round/Hackathon hợp lệ",
                    Map.of("trackId", trackId));
        }
        return createByHackathon(track.getRound().getHackathon().getId(), req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundSummaryResponse> listByHackathon(Integer hackathonId) {
        if (!hackathonRepository.existsById(hackathonId)) {
            throw new ResourceNotFoundException("Hackathon", hackathonId);
        }
        return roundRepository.findByHackathon_IdOrderBySequenceOrderAsc(hackathonId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public List<RoundSummaryResponse> listByTrack(Integer trackId) {
        log.warn("Deprecated API: GET /tracks/{}/rounds", trackId);
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        if (track.getRound() == null) {
            return List.of();
        }
        return List.of(toSummary(track.getRound()));
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
        int criteriaCount;
        float total;
        if (Boolean.TRUE.equals(r.getIsFinal())) {
            criteriaCount = (int) criteriaRepository.countNormalByFinalRoundId(r.getId());
            total = weightSummaryService.rawTotalForFinalRound(r.getId()).orElse(0.0).floatValue();
        } else {
            criteriaCount = 0;
            total = 0f;
        }
        return roundMapper.toSummary(r, criteriaCount, total);
    }

    private void guardHackathonMutable(Hackathon h) {
        if (!MUTABLE_PARENT.contains(h.getStatus())) {
            throw new BusinessRuleException(ErrorCode.TRACK_HACKATHON_LOCKED,
                    "Hackathon status=%s không cho phép sửa cấu trúc Round"
                            .formatted(h.getStatus()),
                    Map.of("hackathonId", h.getId(), "status", h.getStatus()));
        }
    }

    private void validateFinalSequenceOrder(Integer hackathonId, CreateRoundRequest req) {
        if (!Boolean.TRUE.equals(req.getIsFinal()) || req.getSequenceOrder() == null) {
            return;
        }
        int maxPrelim = roundRepository.maxSequenceOrderNonFinal(hackathonId);
        if (req.getSequenceOrder() <= maxPrelim) {
            throw new BusinessRuleException(ErrorCode.ROUND_FINAL_SEQUENCE_ORDER,
                    "Round Chung kết phải đến sau vòng Sơ loại (sequence_order > %d)"
                            .formatted(maxPrelim),
                    Map.of("hackathonId", hackathonId,
                            "sequenceOrder", req.getSequenceOrder(),
                            "maxPreliminarySequence", maxPrelim));
        }
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
