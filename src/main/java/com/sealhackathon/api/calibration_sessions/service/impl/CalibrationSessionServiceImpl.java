package com.sealhackathon.api.calibration_sessions.service.impl;

import com.sealhackathon.api.calibration_sessions.dto.request.CreateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.request.UpdateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.response.CalibrationSessionResponse;
import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.repository.CalibrationSessionRepository;
import com.sealhackathon.api.calibration_sessions.service.CalibrationSessionService;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalibrationSessionServiceImpl implements CalibrationSessionService {

    private final CalibrationSessionRepository calibrationSessionRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;

    @Override
    public CalibrationSessionResponse create(CreateCalibrationSessionRequest req) {
        Round round = roundRepository.findById(req.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round", req.getRoundId()));

        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Không thể mở phiên hiệu chuẩn cho Round đã khóa chấm điểm");
        }

        Track track = null;
        if (req.getTrackId() != null) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Vòng Chung kết không gắn phiên hiệu chuẩn theo bảng đấu");
            }
            track = trackRepository.findById(req.getTrackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Track", req.getTrackId()));
            if (track.getRound() == null || !track.getRound().getId().equals(round.getId())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Bảng đấu không thuộc vòng thi này");
            }
            if (calibrationSessionRepository.existsByRound_IdAndTrack_IdAndStatus(
                    round.getId(), track.getId(), CalibrationStatus.OPEN)) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Đã có phiên hiệu chuẩn OPEN cho bảng này");
            }
        } else if (calibrationSessionRepository.existsByRound_IdAndTrackIsNullAndStatus(
                round.getId(), CalibrationStatus.OPEN)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đã có phiên hiệu chuẩn OPEN cho vòng này");
        }

        Submission sampleSubmission = null;
        if (req.getSampleSubmissionId() != null) {
            sampleSubmission = submissionRepository.findById(req.getSampleSubmissionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Submission", req.getSampleSubmissionId()));

            if (sampleSubmission.getRound() == null || !sampleSubmission.getRound().getId().equals(round.getId())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Bài mẫu không thuộc Vòng thi này");
            }
            if (track != null) {
                Integer sampleTrackId = sampleSubmission.getTrack() != null
                        ? sampleSubmission.getTrack().getId() : null;
                if (sampleTrackId == null || !sampleTrackId.equals(track.getId())) {
                    throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                            "Bài mẫu không thuộc bảng đấu của phiên hiệu chuẩn");
                }
            }
        }

        User creator = null;
        if (currentUserAccessor.currentUserId() != null) {
            creator = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        }

        CalibrationSession session = CalibrationSession.builder()
                .round(round)
                .track(track)
                .sampleSubmission(sampleSubmission)
                .status(CalibrationStatus.OPEN)
                .targetScore(req.getTargetScore())
                .instructions(req.getInstructions())
                .startedAt(LocalDateTime.now())
                .createdBy(creator)
                .build();

        CalibrationSession saved = calibrationSessionRepository.save(session);

        Map<String, Object> auditMeta = new java.util.HashMap<>();
        auditMeta.put("roundId", round.getId());
        auditMeta.put("targetScore", req.getTargetScore());
        if (track != null) {
            auditMeta.put("trackId", track.getId());
        }
        auditService.log(AuditAction.CALIBRATION_SESSION_CREATED, "calibration_sessions", saved.getId(), auditMeta);

        return toResponse(saved);
    }

    @Override
    public CalibrationSessionResponse update(Integer sessionId, UpdateCalibrationSessionRequest req) {
        CalibrationSession session = calibrationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("CalibrationSession", sessionId));

        if (session.getStatus() == CalibrationStatus.CLOSED && req.getStatus() == CalibrationStatus.CLOSED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Phiên hiệu chuẩn này đã được đóng từ trước");
        }

        session.setStatus(req.getStatus());
        if (req.getStatus() == CalibrationStatus.CLOSED) {
            session.setEndedAt(LocalDateTime.now());
        }

        CalibrationSession saved = calibrationSessionRepository.save(session);

        auditService.log(AuditAction.CALIBRATION_SESSION_UPDATED, "calibration_sessions", saved.getId(),
                Map.of("newStatus", req.getStatus().name()));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationSessionResponse> listByRound(Integer roundId) {
        return listByRound(roundId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationSessionResponse> listByRound(Integer roundId, Integer trackId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("Round", roundId);
        }
        List<CalibrationSession> sessions = trackId != null
                ? calibrationSessionRepository.findByRound_IdAndTrack_IdOrderByStartedAtDesc(roundId, trackId)
                : calibrationSessionRepository.findByRound_IdOrderByStartedAtDesc(roundId);
        return sessions.stream()
                .map(CalibrationSessionServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    private static CalibrationSessionResponse toResponse(CalibrationSession session) {
        Track track = session.getTrack();
        return CalibrationSessionResponse.builder()
                .id(session.getId())
                .roundId(session.getRound().getId())
                .trackId(track != null ? track.getId() : null)
                .trackName(track != null ? track.getName() : null)
                .sampleSubmissionId(session.getSampleSubmission() != null ? session.getSampleSubmission().getId() : null)
                .status(session.getStatus())
                .targetScore(session.getTargetScore())
                .instructions(session.getInstructions())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdById(session.getCreatedBy() != null ? session.getCreatedBy().getId() : null)
                .build();
    }
}
