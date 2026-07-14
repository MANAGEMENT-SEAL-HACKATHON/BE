package com.sealhackathon.api.calibration_sessions.service.impl;

import com.sealhackathon.api.calibration_sessions.dto.request.CreateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.request.UpdateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.response.CalibrationSessionResponse;
import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.repository.CalibrationSessionRepository;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalibrationSessionServiceImplTest {

    @Mock private CalibrationSessionRepository calibrationSessionRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private AuditService auditService;

    @InjectMocks
    private CalibrationSessionServiceImpl service;

    @Test
    void create_gd3_withTrack_success() {
        Round round = Round.builder().id(10).scoringLocked(false).isFinal(false).build();
        Track track = Track.builder().id(3).name("BA").round(round).build();
        Submission sample = Submission.builder().id(42).round(round).track(track).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.findById(3)).thenReturn(Optional.of(track));
        when(calibrationSessionRepository.existsByRound_IdAndTrack_IdAndStatus(
                10, 3, CalibrationStatus.OPEN)).thenReturn(false);
        when(submissionRepository.findById(42)).thenReturn(Optional.of(sample));
        when(currentUserAccessor.currentUserId()).thenReturn(null);
        when(calibrationSessionRepository.save(any(CalibrationSession.class))).thenAnswer(inv -> {
            CalibrationSession s = inv.getArgument(0);
            s.setId(1);
            return s;
        });

        CalibrationSessionResponse res = service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .trackId(3)
                .sampleSubmissionId(42)
                .targetScore(80f)
                .build());

        assertThat(res.getId()).isEqualTo(1);
        assertThat(res.getTrackId()).isEqualTo(3);
        assertThat(res.getTrackName()).isEqualTo("BA");
        assertThat(res.getStatus()).isEqualTo(CalibrationStatus.OPEN);
    }

    @Test
    void create_gd3_secondOpenSameTrack_throwsInvalidState() {
        Round round = Round.builder().id(10).scoringLocked(false).isFinal(false).build();
        Track track = Track.builder().id(3).name("BA").round(round).build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.findById(3)).thenReturn(Optional.of(track));
        when(calibrationSessionRepository.existsByRound_IdAndTrack_IdAndStatus(
                10, 3, CalibrationStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .trackId(3)
                .targetScore(80f)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void create_gd5_trackNull_success() {
        Round round = Round.builder().id(10).scoringLocked(false).isFinal(true).build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(calibrationSessionRepository.existsByRound_IdAndTrackIsNullAndStatus(
                eq(10), eq(CalibrationStatus.OPEN))).thenReturn(false);
        when(currentUserAccessor.currentUserId()).thenReturn(null);
        when(calibrationSessionRepository.save(any(CalibrationSession.class))).thenAnswer(inv -> {
            CalibrationSession s = inv.getArgument(0);
            s.setId(1);
            return s;
        });

        CalibrationSessionResponse res = service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .targetScore(80f)
                .instructions("Align rubric")
                .build());

        assertThat(res.getId()).isEqualTo(1);
        assertThat(res.getTrackId()).isNull();
        assertThat(res.getStatus()).isEqualTo(CalibrationStatus.OPEN);
    }

    @Test
    void create_sampleWrongTrack_throwsInvalidState() {
        Round round = Round.builder().id(10).scoringLocked(false).isFinal(false).build();
        Track trackA = Track.builder().id(3).name("A").round(round).build();
        Track trackB = Track.builder().id(4).name("B").round(round).build();
        Submission sample = Submission.builder().id(42).round(round).track(trackB).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.findById(3)).thenReturn(Optional.of(trackA));
        when(calibrationSessionRepository.existsByRound_IdAndTrack_IdAndStatus(
                10, 3, CalibrationStatus.OPEN)).thenReturn(false);
        when(submissionRepository.findById(42)).thenReturn(Optional.of(sample));

        assertThatThrownBy(() -> service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .trackId(3)
                .sampleSubmissionId(42)
                .targetScore(80f)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void create_secondOpenNullTrack_throwsInvalidState() {
        Round round = Round.builder().id(10).scoringLocked(false).isFinal(true).build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(calibrationSessionRepository.existsByRound_IdAndTrackIsNullAndStatus(
                10, CalibrationStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .targetScore(80f)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void create_lockedRound_throwsInvalidState() {
        when(roundRepository.findById(10)).thenReturn(Optional.of(Round.builder()
                .id(10)
                .scoringLocked(true)
                .build()));

        assertThatThrownBy(() -> service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .targetScore(80f)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void update_closeHappy() {
        Round round = Round.builder().id(10).build();
        CalibrationSession session = CalibrationSession.builder()
                .id(1)
                .round(round)
                .status(CalibrationStatus.OPEN)
                .build();
        when(calibrationSessionRepository.findById(1)).thenReturn(Optional.of(session));
        when(calibrationSessionRepository.save(any(CalibrationSession.class))).thenAnswer(inv -> inv.getArgument(0));

        CalibrationSessionResponse res = service.update(1, UpdateCalibrationSessionRequest.builder()
                .status(CalibrationStatus.CLOSED)
                .build());

        assertThat(res.getStatus()).isEqualTo(CalibrationStatus.CLOSED);
        assertThat(res.getEndedAt()).isNotNull();
    }

    @Test
    void update_reClose_throwsInvalidState() {
        Round round = Round.builder().id(10).build();
        when(calibrationSessionRepository.findById(1)).thenReturn(Optional.of(CalibrationSession.builder()
                .id(1)
                .round(round)
                .status(CalibrationStatus.CLOSED)
                .build()));

        assertThatThrownBy(() -> service.update(1, UpdateCalibrationSessionRequest.builder()
                .status(CalibrationStatus.CLOSED)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void create_sampleWrongRound_throwsInvalidState() {
        Round round = Round.builder().id(10).scoringLocked(false).isFinal(true).build();
        Round other = Round.builder().id(11).build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(calibrationSessionRepository.existsByRound_IdAndTrackIsNullAndStatus(
                10, CalibrationStatus.OPEN)).thenReturn(false);
        when(submissionRepository.findById(42)).thenReturn(Optional.of(Submission.builder()
                .id(42)
                .round(other)
                .build()));

        assertThatThrownBy(() -> service.create(CreateCalibrationSessionRequest.builder()
                .roundId(10)
                .sampleSubmissionId(42)
                .targetScore(80f)
                .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }
}
