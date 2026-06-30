package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.presentation.dto.request.PresentationControllerGrantRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationControllerResponse;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.service.PresentationControllerService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationControllerServiceImpl implements PresentationControllerService {

    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final PresentationControllerGuard controllerGuard;
    private final AuditService auditService;

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
        User judge = loadJudge(request.getJudgeId());
        ensureJudgeOnTrack(judge.getId(), trackId);
        track.setControllerJudge(judge);
        trackRepository.save(track);
        auditService.log(AuditAction.PRESENTATION_CONTROLLER_GRANTED, "tracks", trackId,
                Map.of("judgeId", judge.getId()));
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
        round.setControllerJudge(judge);
        roundRepository.save(round);
        auditService.log(AuditAction.PRESENTATION_CONTROLLER_GRANTED, "rounds", roundId,
                Map.of("judgeId", judge.getId()));
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
                    "Judge chưa được phân công cho track");
        }
    }

    private void ensureJudgeOnRound(Integer judgeId, Integer roundId) {
        if (!judgeAssignmentRepository.existsByJudgeIdAndRoundId(judgeId, roundId)) {
            throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                    "Judge chưa được phân công cho round chung kết");
        }
    }

    private PresentationControllerResponse toResponse(Integer judgeId, String source) {
        if (judgeId == null) {
            return PresentationControllerResponse.builder().source(source).build();
        }
        User judge = userRepository.findById(judgeId).orElse(null);
        String fullName = judge != null ? judge.getFullName() : null;
        return PresentationControllerResponse.builder()
                .judgeId(judgeId)
                .judgeName(fullName)
                .judgeFullName(fullName)
                .isDeptHead(judge != null && Boolean.TRUE.equals(judge.getIsDeptHead()))
                .source(source)
                .build();
    }
}
