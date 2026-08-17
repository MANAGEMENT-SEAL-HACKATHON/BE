package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.request.PresentationControllerGrantRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationControllerResponse;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.service.PresentationControllerService;
import com.sealhackathon.api.presentation.support.JudgePresenceRegistry;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationControllerServiceImpl implements PresentationControllerService {

    private static final long ONLINE_STATUS_SECONDS = 90;

    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final PresentationControllerGuard controllerGuard;
    private final AuditService auditService;
    private final JudgePresenceRegistry presenceRegistry;
    private final PresentationQueuePublisher queuePublisher;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    @Transactional(readOnly = true)
    public PresentationControllerResponse getTrackController(Integer trackId) {
        Track track = loadTrack(trackId);
        Integer controllerId = controllerGuard.resolveTrackControllerId(track);
        String source;
        if (track.getControllerJudge() != null) {
            source = "OVERRIDE";
        } else if (controllerId != null) {
            source = "AUTO_DEFAULT";
        } else {
            source = "UNASSIGNED";
        }
        return toResponse(controllerId, source);
    }

    @Override
    public PresentationControllerResponse grantTrackController(Integer trackId, PresentationControllerGrantRequest request) {
        Track track = loadTrack(trackId);
        Round round = track.getRound();
        User judge = loadJudge(request.getJudgeId());
        ensureJudgeOnTrack(judge.getId(), trackId);
        Integer previousId = track.getControllerJudge() != null ? track.getControllerJudge().getId() : null;
        // Compare against resolved controller (OVERRIDE or AUTO_DEFAULT) — FE sends the same from GET
        assertExpectedController(controllerGuard.resolveTrackControllerId(track), request.getExpectedControllerJudgeId());

        track.setControllerJudge(judge);
        trackRepository.save(track);

        Map<String, Object> detail = new HashMap<>();
        detail.put("judgeId", judge.getId());
        detail.put("previousJudgeId", previousId);
        detail.put("mode", "TRANSFER");
        auditService.log(AuditAction.PRESENTATION_CONTROLLER_GRANTED, "tracks", trackId, detail);

        queuePublisher.publishControllerChanged(round.getId(), trackId, judge.getId(), previousId);
        return toResponse(judge.getId(), "OVERRIDE");
    }

    @Override
    public void revokeTrackController(Integer trackId) {
        Track track = loadTrack(trackId);
        if (track.getControllerJudge() == null) {
            return;
        }
        Integer previous = track.getControllerJudge().getId();
        track.setControllerJudge(null);
        trackRepository.save(track);
        auditService.log(AuditAction.PRESENTATION_CONTROLLER_REVOKED, "tracks", trackId,
                Map.of("judgeId", previous));
        queuePublisher.publishControllerChanged(track.getRound().getId(), trackId, null, previous);
    }

    @Override
    @Transactional(readOnly = true)
    public PresentationControllerResponse getRoundController(Integer roundId) {
        Round round = loadRound(roundId);
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Controller round chỉ áp dụng cho vòng chung kết");
        }
        return toResponse(controllerGuard.resolveRoundControllerId(round),
                round.getControllerJudge() != null ? "OVERRIDE" : "UNASSIGNED");
    }

    @Override
    public PresentationControllerResponse grantRoundController(Integer roundId, PresentationControllerGrantRequest request) {
        Round round = loadRound(roundId);
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Controller round chỉ áp dụng cho vòng chung kết");
        }
        User judge = loadJudge(request.getJudgeId());
        ensureJudgeOnRound(judge.getId(), roundId);
        Integer previousId = round.getControllerJudge() != null ? round.getControllerJudge().getId() : null;
        // Compare against resolved controller (OVERRIDE or AUTO_DEFAULT) — FE sends the same from GET
        assertExpectedController(controllerGuard.resolveRoundControllerId(round), request.getExpectedControllerJudgeId());

        round.setControllerJudge(judge);
        roundRepository.save(round);

        Map<String, Object> detail = new HashMap<>();
        detail.put("judgeId", judge.getId());
        detail.put("previousJudgeId", previousId);
        detail.put("mode", "TRANSFER");
        auditService.log(AuditAction.PRESENTATION_CONTROLLER_GRANTED, "rounds", roundId, detail);

        queuePublisher.publishControllerChanged(roundId, null, judge.getId(), previousId);
        return toResponse(judge.getId(), "OVERRIDE");
    }

    @Override
    public void revokeRoundController(Integer roundId) {
        Round round = loadRound(roundId);
        if (round.getControllerJudge() == null) {
            return;
        }
        Integer previous = round.getControllerJudge().getId();
        round.setControllerJudge(null);
        roundRepository.save(round);
        auditService.log(AuditAction.PRESENTATION_CONTROLLER_REVOKED, "rounds", roundId,
                Map.of("judgeId", previous));
        queuePublisher.publishControllerChanged(roundId, null, null, previous);
    }

    @Override
    public void heartbeat(Integer roundId, Integer trackId) {
        Integer userId = currentUserAccessor.currentUserId();
        presenceRegistry.heartbeat(userId);
    }

    private void assertExpectedController(Integer currentId, Integer expectedId) {
        if (expectedId == null) {
            return;
        }
        // expectedId == 0 means expect no resolved controller (UNASSIGNED)
        if (expectedId == 0) {
            if (currentId != null) {
                throw new ConflictException(ErrorCode.CONTROLLER_CONFLICT,
                        "Controller đã được chuyển bởi người khác",
                        conflictDetails(currentId, expectedId));
            }
            return;
        }
        if (!java.util.Objects.equals(currentId, expectedId)) {
            throw new ConflictException(ErrorCode.CONTROLLER_CONFLICT,
                    "Controller đã được chuyển bởi người khác",
                    conflictDetails(currentId, expectedId));
        }
    }

    /** Null-safe detail map — {@code Map.of} rejects null values and would NPE. */
    static Map<String, Object> conflictDetails(Integer currentId, Integer expectedId) {
        Map<String, Object> details = new HashMap<>();
        if (currentId != null) {
            details.put("currentControllerJudgeId", currentId);
        }
        if (expectedId != null) {
            details.put("expectedControllerJudgeId", expectedId);
        }
        return details;
    }

    private Track loadTrack(Integer trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
    }

    private Round loadRound(Integer roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
    }

    private User loadJudge(Integer judgeId) {
        return userRepository.findById(judgeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", judgeId));
    }

    private void ensureJudgeOnTrack(Integer judgeId, Integer trackId) {
        if (!judgeAssignmentRepository.existsByJudgeIdAndTrackId(judgeId, trackId)) {
            throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED_TO_TRACK,
                    "Giám khảo chưa được phân công vào bảng đấu này — không thể trao quyền điều khiển",
                    Map.of("judgeId", judgeId, "trackId", trackId));
        }
    }

    private void ensureJudgeOnRound(Integer judgeId, Integer roundId) {
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundId(judgeId, roundId)) {
            throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                    "Giám khảo chưa được phân công vào vòng Chung kết này — không thể trao quyền điều khiển",
                    Map.of("judgeId", judgeId, "roundId", roundId));
        }
    }

    private PresentationControllerResponse toResponse(Integer judgeId, String source) {
        if (judgeId == null) {
            return PresentationControllerResponse.builder().source(source).online(false).build();
        }
        User judge = userRepository.findById(judgeId).orElse(null);
        String fullName = judge != null ? judge.getFullName() : null;
        Instant lastSeen = presenceRegistry.lastSeenAt(judgeId);
        boolean online = presenceRegistry.isOnline(judgeId, ONLINE_STATUS_SECONDS);
        return PresentationControllerResponse.builder()
                .judgeId(judgeId)
                .judgeName(fullName)
                .judgeFullName(fullName)
                .isDeptHead(judge != null && Boolean.TRUE.equals(judge.getIsDeptHead()))
                .source(source)
                .lastSeenAt(lastSeen != null ? lastSeen.toString() : null)
                .online(online)
                .build();
    }
}
