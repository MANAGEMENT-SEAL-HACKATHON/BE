package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.request.PresentationQueueNextRequest;
import com.sealhackathon.api.presentation.dto.request.PresentationShuffleRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationShuffleResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationTimerBlock;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.guard.PresentationForceAdvanceAckGuard;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.support.PresentationNextScoringGuard;
import com.sealhackathon.api.presentation.support.PresentationQaTimeoutMaterializer;
import com.sealhackathon.api.presentation.support.PresentationSlotHelper;
import com.sealhackathon.api.presentation.support.PresentationTimerCalculator;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PresentationQueueServiceImpl implements PresentationQueueService {

    private final RoundRepository roundRepository;
    private final HackathonRepository hackathonRepository;
    private final TrackRepository trackRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final PresentationDurationResolver durationResolver;
    private final PresentationControllerGuard controllerGuard;
    private final PresentationForceAdvanceAckGuard forceAdvanceAckGuard;
    private final AuditService auditService;
    private final PresentationQueuePublisher queuePublisher;
    private final CurrentUserAccessor currentUserAccessor;
    private final PresentationNextScoringGuard nextScoringGuard;
    private final JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final RoundPhaseResolver roundPhaseResolver;

    @Override
    public PresentationQueueResponse getQueue(Integer roundId, Integer trackIdFilter) {
        boolean anonymous = isJudgeAnonymousView();
        Round round = resolveRound(roundId);
        assertCanViewQueue(round);
        List<PresentationQueueResponse.TrackQueueItem> trackItems = new ArrayList<>();

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            trackItems.add(buildFinalTrackItem(round, anonymous));
        } else {
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId());
            for (Track track : tracks) {
                if (trackIdFilter != null && !track.getId().equals(trackIdFilter)) {
                    continue;
                }
                trackItems.add(buildTrackItem(round, track, anonymous));
            }
        }

        if (trackIdFilter != null && trackItems.isEmpty()) {
            throw new ResourceNotFoundException("Track", trackIdFilter);
        }

        int total = 0;
        int done = 0;
        int absent = 0;
        for (PresentationQueueResponse.TrackQueueItem track : trackItems) {
            for (PresentationQueueResponse.QueueItem item : track.getItems()) {
                total++;
                if (PresentationQueueStatus.DONE.name().equals(item.getStatus())) {
                    done++;
                }
                if (PresentationQueueStatus.ELIMINATED.name().equals(item.getStatus())
                        || PresentationQueueStatus.SKIPPED.name().equals(item.getStatus())) {
                    absent++;
                }
            }
        }

        return PresentationQueueResponse.builder()
                .roundId(round.getId())
                .tracks(trackItems)
                .roomStats(PresentationQueueResponse.RoomStats.builder()
                        .total(total)
                        .done(done)
                        .absent(absent)
                        .build())
                .build();
    }

    @Override
    public PresentationQueueNextResponse advanceNext(
            Integer roundId,
            Integer trackId,
            Integer currentSubmissionId,
            Integer currentTeamId,
            boolean acknowledgeIncompleteScoring) {
        Round round = resolveRound(roundId);
        requireJudgingPhase(round);
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            controllerGuard.requireControllerForRound(round.getId(), round);
            PresentationQueueNextResponse response = advanceForScope(
                    round, null, currentSubmissionId, currentTeamId, acknowledgeIncompleteScoring);
            queuePublisher.publish(round.getId(), null, getQueue(round.getId(), null));
            return response;
        }
        if (trackId == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "trackId bắt buộc cho vòng không phải chung kết");
        }
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        if (!track.getRound().getId().equals(round.getId())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Track không thuộc round");
        }
        controllerGuard.requireControllerForTrack(trackId, track, round);
        PresentationQueueNextResponse response = advanceForScope(
                round, trackId, currentSubmissionId, currentTeamId, acknowledgeIncompleteScoring);
        PresentationQueueResponse payload = getQueue(roundId, trackId);
        queuePublisher.publish(roundId, trackId, payload);
        return response;
    }

    @Override
    public PresentationQueueNextResponse advanceNextFromRequest(
            Integer roundId,
            Integer trackIdParam,
            PresentationQueueNextRequest request) {
        Integer currentSubmissionId = null;
        Integer currentTeamId = null;
        boolean acknowledgeIncompleteScoring = false;
        String forceAckReason = null;
        Integer trackId = trackIdParam;
        if (request != null) {
            currentSubmissionId = request.getCurrentSubmissionId();
            currentTeamId = request.getCurrentTeamId();
            if (trackId == null) {
                trackId = request.getTrackId();
            }
            if (Boolean.TRUE.equals(request.getAcknowledgeIncompleteScoring())) {
                acknowledgeIncompleteScoring = true;
                forceAckReason = request.getForceAckReason();
            }
        }
        if (acknowledgeIncompleteScoring) {
            if (forceAckReason == null || forceAckReason.isBlank()) {
                throw new BusinessRuleException(
                        ErrorCode.VALIDATION_FAILED,
                        "Force chuyển đội khi chưa đủ chấm bắt buộc nhập lý do (forceAckReason).");
            }
        }
        PresentationQueueNextResponse response = advanceNext(
                roundId, trackId, currentSubmissionId, currentTeamId, acknowledgeIncompleteScoring);
        if (acknowledgeIncompleteScoring) {
            auditService.log(
                    AuditAction.PRESENTATION_FORCE_ADVANCE_ACK,
                    "presentation_queue",
                    roundId,
                    Map.of(
                            "trackId", trackId != null ? trackId : 0,
                            "submissionId", currentSubmissionId != null ? currentSubmissionId : 0,
                            "forceAckReason", forceAckReason.trim(),
                            "actorUserId", currentUserAccessor.currentUserId() != null
                                    ? currentUserAccessor.currentUserId() : 0));
        }
        return response;
    }

    @Override
    public PresentationShuffleResponse shuffle(PresentationShuffleRequest request) {
        Integer roundId = request.getRoundId();
        Round round = resolveRound(roundId);
        requireJudgingPhase(round);
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Round đã khóa — không thể shuffle queue");
        }

        List<PresentationShuffleResponse.TrackShuffleResult> results = new ArrayList<>();

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            controllerGuard.requireControllerForRound(round.getId(), round);
            if (Boolean.TRUE.equals(round.getPresentationShuffled())) {
                List<PresentationSlot> existing = presentationSlotRepository
                        .findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(round.getId());
                if (!existing.isEmpty()) {
                    throw new ConflictException(ErrorCode.PRESENTATION_ALREADY_SHUFFLED,
                            "Hàng đợi chung kết đã được quay số",
                            Map.of("roundId", roundId, "slotCount", existing.size()));
                }
            }
            assertNoPresentationStarted(round.getId(), null);
            scoringConfirmationRepository.deleteByFinalRoundScope(round.getId());
            int count = shuffleFinalRound(round);
            round.setPresentationShuffled(true);
            roundRepository.save(round);
            results.add(PresentationShuffleResponse.TrackShuffleResult.builder()
                    .trackId(null)
                    .slotCount(count)
                    .shuffled(true)
                    .build());
            auditService.log(AuditAction.PRESENTATION_QUEUE_SHUFFLE, "rounds", round.getId(),
                    Map.of("scope", "final", "slotCount", count));
            PresentationQueueResponse payload = getQueue(roundId, null);
            queuePublisher.publish(roundId, null, payload);
            return PresentationShuffleResponse.builder().roundId(roundId).tracks(results).build();
        }

        List<Track> tracks = resolveTracksToShuffle(round, request.getTrackIds());
        for (Track track : tracks) {
            controllerGuard.requireControllerForTrack(track.getId(), track, round);
            if (Boolean.TRUE.equals(track.getPresentationShuffled())) {
                List<PresentationSlot> existing = presentationSlotRepository
                        .findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(roundId, track.getId());
                if (!existing.isEmpty()) {
                    throw new ConflictException(ErrorCode.PRESENTATION_ALREADY_SHUFFLED,
                            "Hàng đợi bảng đấu đã được quay số",
                            Map.of("roundId", roundId, "trackId", track.getId(), "slotCount", existing.size()));
                }
            }
            assertNoPresentationStarted(roundId, track.getId());
            scoringConfirmationRepository.deleteByTrackScope(roundId, track.getId());
            int count = shuffleTrack(round, track);
            track.setPresentationShuffled(true);
            trackRepository.save(track);
            results.add(PresentationShuffleResponse.TrackShuffleResult.builder()
                    .trackId(track.getId())
                    .slotCount(count)
                    .shuffled(true)
                    .build());
            auditService.log(AuditAction.PRESENTATION_QUEUE_SHUFFLE, "tracks", track.getId(),
                    Map.of("roundId", roundId, "slotCount", count));
            PresentationQueueResponse payload = getQueue(roundId, track.getId());
            queuePublisher.publish(roundId, track.getId(), payload);
        }

        return PresentationShuffleResponse.builder().roundId(roundId).tracks(results).build();
    }

    @Override
    public PresentationQueueResponse skipNoShow(Integer roundId, Integer trackId, Integer submissionId) {
        Round round = resolveRound(roundId);
        requireJudgingPhase(round);
        if (submissionId == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "submissionId bắt buộc");
        }
        Integer resolvedTrackId = trackId;
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            controllerGuard.requireControllerForRound(round.getId(), round);
            resolvedTrackId = null;
        } else {
            if (resolvedTrackId == null) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "trackId bắt buộc cho vòng không phải chung kết");
            }
            final Integer trackIdForLoad = resolvedTrackId;
            Track track = trackRepository.findById(trackIdForLoad)
                    .orElseThrow(() -> new ResourceNotFoundException("Track", trackIdForLoad));
            if (!track.getRound().getId().equals(round.getId())) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Track không thuộc round");
            }
            controllerGuard.requireControllerForTrack(trackIdForLoad, track, round);
        }

        List<PresentationSlot> slots = loadSlots(round.getId(), resolvedTrackId);
        PresentationSlot slot = slots.stream()
                .filter(s -> s.getSubmission() != null && s.getSubmission().getId().equals(submissionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PresentationSlot for submission", submissionId));
        if (slot.getQueueStatus() == PresentationQueueStatus.DONE) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Đội đã thuyết trình xong — không skip no-show");
        }
        slot.setQueueStatus(PresentationQueueStatus.SKIPPED);
        slot.setTimerPhase(PresentationTimerPhase.ENDED);
        presentationSlotRepository.save(slot);
        Map<String, Object> skipDetail = new HashMap<>();
        skipDetail.put("roundId", round.getId());
        skipDetail.put("trackId", resolvedTrackId);
        skipDetail.put("submissionId", submissionId);
        auditService.log(AuditAction.PRESENTATION_NO_SHOW_SKIPPED, "presentation_slots", slot.getId(),
                skipDetail);
        PresentationQueueResponse payload = getQueue(round.getId(), resolvedTrackId);
        queuePublisher.publish(round.getId(), resolvedTrackId, payload);
        return payload;
    }

    @Override
    public boolean appendLateApprovedIfShuffled(Submission submission) {
        if (submission == null || !SubmissionGradablePolicy.isGradable(submission)) {
            return false;
        }
        Round round = submission.getRound();
        if (round == null) {
            return false;
        }
        Track track = submission.getTrack();
        Integer trackId = track != null ? track.getId() : null;
        boolean shuffled = Boolean.TRUE.equals(round.getIsFinal())
                ? Boolean.TRUE.equals(round.getPresentationShuffled())
                : track != null && Boolean.TRUE.equals(track.getPresentationShuffled());
        if (!shuffled) {
            return false;
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            if (presentationSlotRepository.findByRound_IdAndSubmission_Id(round.getId(), submission.getId())
                    .isPresent()) {
                return false;
            }
        } else if (trackId != null) {
            boolean exists = presentationSlotRepository
                    .findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(round.getId(), trackId).stream()
                    .anyMatch(s -> s.getSubmission() != null
                            && s.getSubmission().getId().equals(submission.getId()));
            if (exists) {
                return false;
            }
        }

        List<PresentationSlot> existing = trackId == null
                ? presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(round.getId())
                : presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(round.getId(), trackId);
        int nextOrder = existing.stream()
                .mapToInt(s -> s.getSequenceOrder() != null ? s.getSequenceOrder() : 0)
                .max()
                .orElse(0) + 1;
        int slotMinutes = durationResolver.slotMinutes(track, round);
        LocalDateTime start = LocalDateTime.now().withSecond(0).withNano(0);
        if (!existing.isEmpty()) {
            PresentationSlot last = existing.get(existing.size() - 1);
            if (last.getEndsAt() != null) {
                start = last.getEndsAt();
            }
        }
        presentationSlotRepository.save(PresentationSlot.builder()
                .round(round)
                .track(track)
                .submission(submission)
                .team(submission.getTeam())
                .startsAt(start)
                .endsAt(start.plusMinutes(slotMinutes))
                .location(defaultLocation(submission.getTeam().getId()))
                .sequenceOrder(nextOrder)
                .queueStatus(PresentationQueueStatus.WAITING)
                .timerPhase(PresentationTimerPhase.IDLE)
                .pausedAccumulatedSeconds(0)
                .build());
        auditService.log(AuditAction.PRESENTATION_QUEUE_SHUFFLE, "presentation_slots", submission.getId(),
                Map.of("action", "late_append", "roundId", round.getId(), "sequenceOrder", nextOrder));
        PresentationQueueResponse payload = getQueue(round.getId(), trackId);
        queuePublisher.publish(round.getId(), trackId, payload);
        return true;
    }

    /** Cấm shuffle lại nếu đã có slot PRESENTING hoặc DONE. */
    private void assertNoPresentationStarted(Integer roundId, Integer trackId) {
        List<PresentationSlot> slots = trackId == null
                ? presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(roundId)
                : presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(roundId, trackId);
        boolean started = slots.stream().anyMatch(s ->
                s.getQueueStatus() == PresentationQueueStatus.PRESENTING
                        || s.getQueueStatus() == PresentationQueueStatus.DONE
                        || s.getQueueStatus() == PresentationQueueStatus.SKIPPED);
        if (started) {
            throw new BusinessRuleException(ErrorCode.PRESENTATION_ALREADY_STARTED,
                    "Đã bắt đầu thuyết trình — không xáo lại hàng đợi",
                    Map.of("roundId", roundId, "trackId", trackId));
        }
    }

    private PresentationQueueNextResponse advanceForScope(
            Round round,
            Integer trackId,
            Integer currentSubmissionId,
            Integer currentTeamId,
            boolean acknowledgeIncompleteScoring) {
        List<PresentationSlot> slots = loadSlots(round.getId(), trackId);
        if (slots.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Chưa có lịch thuyết trình cho phạm vi này");
        }

        PresentationSlot presenting = slots.stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.PRESENTING)
                .findFirst()
                .orElse(null);

        if (presenting != null) {
            // Race guard: reload with lock so concurrent Next/Early-QA không ghi đè lẫn nhau
            presenting = presentationSlotRepository.findByIdForUpdate(presenting.getId()).orElse(presenting);
        }

        if (presenting == null) {
            if (currentSubmissionId != null) {
                presenting = slots.stream()
                        .filter(s -> s.getSubmission() != null
                                && s.getSubmission().getId().equals(currentSubmissionId))
                        .findFirst()
                        .orElse(null);
            } else if (currentTeamId != null) {
                presenting = slots.stream()
                        .filter(s -> s.getTeam().getId().equals(currentTeamId))
                        .findFirst()
                        .orElse(null);
            }
            if (presenting != null) {
                presenting.setQueueStatus(PresentationQueueStatus.PRESENTING);
                resetTimer(presenting);
                presentationSlotRepository.save(presenting);
            }
        }

        PresentationQueueNextResponse.ScoringSnapshot scoringSnapshot = null;
        if (presenting != null) {
            // Lazy auto-timeout trước khi kiểm tra phase
            Track trackForTimer = presenting.getTrack();
            PresentationQaTimeoutMaterializer.materializeIfExpired(
                    presenting, trackForTimer, round, durationResolver, presentationSlotRepository);
            if (presenting.getSubmission() != null) {
                PresentationTimerPhase phase = presenting.getTimerPhase();
                if (phase != PresentationTimerPhase.ENDED) {
                    throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                            "Chỉ chuyển đội tiếp khi phần thuyết trình/Q&A đã kết thúc (ENDED)");
                }
                boolean ack = forceAdvanceAckGuard.resolveAcknowledge(
                        acknowledgeIncompleteScoring, trackId, round);
                // Kết thúc sớm Q&A → bắt đủ chốt (trừ force-ack). Hết giờ tự nhiên → thiếu điểm OK.
                // null qaEndedEarly (slot cũ): giữ hành vi cũ = require complete.
                boolean requireComplete = !Boolean.FALSE.equals(presenting.getQaEndedEarly());
                nextScoringGuard.validateBeforeNext(
                        presenting.getSubmission(), trackId, round, ack, requireComplete);
                scoringSnapshot = nextScoringGuard.snapshot(
                        presenting.getSubmission(), trackId, round);
            }
            presenting.setQueueStatus(PresentationQueueStatus.DONE);
            presenting.setTimerPhase(PresentationTimerPhase.ENDED);
            presentationSlotRepository.save(presenting);
        }

        PresentationSlot next = slots.stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.WAITING)
                .findFirst()
                .orElse(null);

        if (next != null) {
            if (next.getSubmission() != null) {
                // Reset judge confirmations for the new live slot.
                scoringConfirmationRepository.deleteBySubmission_Id(next.getSubmission().getId());
            }
            next.setQueueStatus(PresentationQueueStatus.PRESENTING);
            resetTimer(next);
            next.setTimerPhase(PresentationTimerPhase.SETUP);
            presentationSlotRepository.save(next);
            return PresentationQueueNextResponse.builder()
                    .trackId(trackId)
                    .nextSubmissionId(next.getSubmission() != null ? next.getSubmission().getId() : null)
                    .nextTeamId(next.getTeam().getId())
                    .completedSubmissionScoring(scoringSnapshot)
                    .build();
        }

        return PresentationQueueNextResponse.builder()
                .trackId(trackId)
                .nextSubmissionId(null)
                .nextTeamId(null)
                .completedSubmissionScoring(scoringSnapshot)
                .build();
    }

    private int shuffleTrack(Round round, Track track) {
        presentationSlotRepository.deleteByRound_IdAndTrack_Id(round.getId(), track.getId());

        List<Submission> gradable = submissionRepository.findByTrack_Round_Id(round.getId()).stream()
                .filter(s -> s.getTrack() != null && s.getTrack().getId().equals(track.getId()))
                .filter(SubmissionGradablePolicy::isGradable)
                .collect(Collectors.toCollection(ArrayList::new));
        fisherYatesShuffle(gradable);

        return createSlotsFromSubmissions(round, track, gradable);
    }

    private int shuffleFinalRound(Round round) {
        presentationSlotRepository.deleteByRound_IdAndTrackIsNull(round.getId());

        List<Submission> gradable = new ArrayList<>();
        for (TeamRoundParticipation trp : teamRoundParticipationRepository.findByRound_Id(round.getId())) {
            submissionRepository.findTopByTeam_IdAndRound_IdOrderBySubmittedAtDesc(
                            trp.getTeam().getId(), round.getId())
                    .filter(SubmissionGradablePolicy::isGradable)
                    .ifPresent(gradable::add);
        }
        fisherYatesShuffle(gradable);
        return createSlotsFromSubmissions(round, null, gradable);
    }

    private int createSlotsFromSubmissions(Round round, Track track, List<Submission> submissions) {
        LocalDateTime examAt = round.getExamAt() != null
                ? round.getExamAt()
                : LocalDateTime.now().withSecond(0).withNano(0);
        int slotMinutes = durationResolver.slotMinutes(track, round);
        int order = 1;
        for (Submission submission : submissions) {
            LocalDateTime start = examAt.plusMinutes((long) (order - 1) * slotMinutes);
            PresentationQueueStatus status = order == 1
                    ? PresentationQueueStatus.PRESENTING
                    : PresentationQueueStatus.WAITING;
            presentationSlotRepository.save(PresentationSlot.builder()
                    .round(round)
                    .track(track)
                    .submission(submission)
                    .team(submission.getTeam())
                    .startsAt(start)
                    .endsAt(start.plusMinutes(slotMinutes))
                    .location(defaultLocation(submission.getTeam().getId()))
                    .sequenceOrder(order)
                    .queueStatus(status)
                    .timerPhase(PresentationTimerPhase.IDLE)
                    .pausedAccumulatedSeconds(0)
                    .build());
            order++;
        }
        return submissions.size();
    }

    private boolean isJudgeAnonymousView() {
        var user = currentUserAccessor.currentUser();
        return user != null && user.getRole() == UserRole.JUDGE;
    }

    /**
     * Chống IDOR/snooping cross-hackathon: STUDENT chỉ xem được hàng đợi của hackathon mình
     * có đăng ký. Coordinator/Judge/Mentor (staff) được xem để điều phối/chấm/theo dõi
     * (Judge đã bị ẩn danh tên đội qua {@link #isJudgeAnonymousView()}).
     */
    private void assertCanViewQueue(Round round) {
        var user = currentUserAccessor.currentUser();
        if (user == null) {
            return;
        }
        UserRole role = user.getRole();
        if (role == UserRole.COORDINATOR || role == UserRole.JUDGE || role == UserRole.MENTOR) {
            return;
        }
        Integer hackathonId = round.getHackathon() != null ? round.getHackathon().getId() : null;
        if (hackathonId != null
                && hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathonId, user.getUserId())) {
            return;
        }
        throw new AuthException(ErrorCode.FORBIDDEN,
                "Bạn không có quyền xem hàng đợi thuyết trình của hackathon này",
                HttpStatus.FORBIDDEN);
    }

    private PresentationQueueResponse.TrackQueueItem buildTrackItem(Round round, Track track, boolean anonymous) {
        List<PresentationSlot> slots = presentationSlotRepository
                .findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(round.getId(), track.getId());
        return PresentationQueueResponse.TrackQueueItem.builder()
                .trackId(track.getId())
                .trackName(track.getName())
                .shuffled(Boolean.TRUE.equals(track.getPresentationShuffled()))
                .items(slots.stream().map(slot -> toQueueItem(slot, track, round, anonymous)).toList())
                .build();
    }

    private PresentationQueueResponse.TrackQueueItem buildFinalTrackItem(Round round, boolean anonymous) {
        List<PresentationSlot> slots = presentationSlotRepository
                .findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(round.getId());
        Integer participatingTeamCount = null;
        Integer gradableTeamCount = null;
        List<PresentationQueueResponse.EligibleTeamItem> eligibleTeams = null;

        if (!anonymous) {
            List<TeamRoundParticipation> participants =
                    teamRoundParticipationRepository.findByRound_Id(round.getId());
            participatingTeamCount = participants.size();
            eligibleTeams = new ArrayList<>();
            int gradable = 0;
            for (TeamRoundParticipation participation : participants) {
                Team team = participation.getTeam();
                var submissionOpt = submissionRepository.findTopByTeam_IdAndRound_IdOrderBySubmittedAtDesc(
                        team.getId(), round.getId());
                boolean isGradable = submissionOpt.filter(SubmissionGradablePolicy::isGradable).isPresent();
                if (isGradable) {
                    gradable++;
                }
                eligibleTeams.add(PresentationQueueResponse.EligibleTeamItem.builder()
                        .teamId(team.getId())
                        .teamName(team.getTeamName())
                        .gradable(isGradable)
                        .submissionStatus(submissionOpt.map(s -> s.getStatus().name()).orElse(null))
                        .build());
            }
            gradableTeamCount = gradable;
            warnHardLockLateInvariant(round, eligibleTeams);
        }

        return PresentationQueueResponse.TrackQueueItem.builder()
                .trackId(null)
                .trackName("Chung kết")
                .shuffled(Boolean.TRUE.equals(round.getPresentationShuffled()))
                .items(slots.stream().map(slot -> toQueueItem(slot, null, round, anonymous)).toList())
                .participatingTeamCount(participatingTeamCount)
                .gradableTeamCount(gradableTeamCount)
                .eligibleTeams(eligibleTeams)
                .build();
    }

    private void warnHardLockLateInvariant(
            Round round, List<PresentationQueueResponse.EligibleTeamItem> eligibleTeams) {
        if (round == null || eligibleTeams == null) {
            return;
        }
        boolean hardLock = Boolean.TRUE.equals(round.getIsFinal())
                || round.getLateSubmissionPolicy() == com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy.HARD_LOCK;
        if (!hardLock) {
            return;
        }
        for (PresentationQueueResponse.EligibleTeamItem item : eligibleTeams) {
            String status = item.getSubmissionStatus();
            if (status == null) {
                continue;
            }
            String n = status.toUpperCase();
            if ("LATE_PENDING".equals(n) || "LATE_APPROVED".equals(n)) {
                log.warn("[INVARIANT_VIOLATION] HARD_LOCK_LATE_STATUS roundId={} teamId={} status={}",
                        round.getId(), item.getTeamId(), n);
                auditService.log(AuditAction.INVARIANT_VIOLATION_HARD_LOCK_LATE, "rounds", round.getId(),
                        Map.of("teamId", item.getTeamId(), "status", n));
            }
        }
    }

    private PresentationQueueResponse.QueueItem toQueueItem(
            PresentationSlot slot, Track track, Round round, boolean anonymous) {
        Integer submissionId = slot.getSubmission() != null ? slot.getSubmission().getId() : null;
        return PresentationQueueResponse.QueueItem.builder()
                .submissionId(submissionId)
                .displayCode(submissionId != null ? "#" + submissionId : null)
                .teamId(anonymous ? null : slot.getTeam().getId())
                .teamName(anonymous ? null : slot.getTeam().getTeamName())
                .order(slot.getSequenceOrder())
                .status(slot.getQueueStatus() != null ? slot.getQueueStatus().name() : PresentationQueueStatus.WAITING.name())
                .presentationSchedule(PresentationSlotHelper.formatSchedule(slot.getStartsAt(), slot.getEndsAt()))
                .location(slot.getLocation())
                .timer(buildTimerBlock(slot, track, round))
                .build();
    }

    private PresentationTimerBlock buildTimerBlock(PresentationSlot slot, Track track, Round round) {
        PresentationQaTimeoutMaterializer.materializeIfExpired(
                slot, track, round, durationResolver, presentationSlotRepository);
        PresentationTimerPhase phase = slot.getTimerPhase() != null ? slot.getTimerPhase() : PresentationTimerPhase.IDLE;
        return PresentationTimerBlock.builder()
                .phase(phase.name())
                .presentationMinutes(durationResolver.presentationMinutes(track, round))
                .qaMinutes(durationResolver.qaMinutes(track, round))
                .presentationStartedAt(slot.getPresentationStartedAt())
                .qaStartedAt(slot.getQaStartedAt())
                .pausedAt(slot.getPausedAt())
                .pausedAccumulatedSeconds(slot.getPausedAccumulatedSeconds())
                .remainingSeconds(PresentationTimerCalculator.remainingSeconds(slot, track, round, durationResolver))
                .build();
    }

    private List<Track> resolveTracksToShuffle(Round round, List<Integer> trackIds) {
        List<Track> all = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId());
        if (trackIds == null || trackIds.isEmpty()) {
            return all;
        }
        Map<Integer, Track> byId = all.stream().collect(Collectors.toMap(Track::getId, t -> t));
        List<Track> selected = new ArrayList<>();
        for (Integer id : trackIds) {
            Track track = byId.get(id);
            if (track == null) {
                throw new ResourceNotFoundException("Track", id);
            }
            selected.add(track);
        }
        return selected;
    }

    private List<PresentationSlot> loadSlots(Integer roundId, Integer trackId) {
        if (trackId == null) {
            return presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(roundId);
        }
        return presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(roundId, trackId);
    }

    private void fisherYatesShuffle(List<Submission> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            Collections.swap(list, i, j);
        }
    }

    private static void resetTimer(PresentationSlot slot) {
        slot.setTimerPhase(PresentationTimerPhase.IDLE);
        slot.setTimerPhaseBeforePause(null);
        slot.setPresentationStartedAt(null);
        slot.setQaStartedAt(null);
        slot.setPausedAt(null);
        slot.setPausedAccumulatedSeconds(0);
        slot.setQaEndedEarly(null);
    }

    private static String defaultLocation(Integer teamId) {
        int room = teamId != null ? (teamId % 3 + 1) : 1;
        return "Online (Teams) - Phòng " + room;
    }

    private void requireJudgingPhase(Round round) {
        RoundPhase phase = roundPhaseResolver.resolve(round);
        if (phase == RoundPhase.JUDGING) {
            return;
        }
        String message = switch (phase) {
            case SETUP -> "Vòng thi chưa được kích hoạt — không xáo hàng đợi thuyết trình";
            case SCORING_LOCKED -> "Vòng đã khóa chấm — không xáo hàng đợi thuyết trình";
            case PUBLISHED -> "Vòng đã công bố kết quả — không xáo hàng đợi thuyết trình";
            default -> "Chưa hết hạn nộp bài — không xáo hàng đợi thuyết trình";
        };
        throw new BusinessRuleException(ErrorCode.SUBMISSION_NOT_CLOSED_FOR_SHUFFLE,
                message,
                Map.of("roundId", round.getId(),
                        "submissionDeadline", round.getSubmissionDeadline(),
                        "phase", phase.name()));
    }

    private Round resolveRound(Integer roundId) {
        if (roundId != null) {
            return roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        }
        var ongoing = hackathonRepository.search(
                HackathonStatus.ONGOING, null, null, null, PageRequest.of(0, 1));
        if (!ongoing.hasContent()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Không có hackathon ONGOING");
        }
        Integer hackathonId = ongoing.getContent().get(0).getId();
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE, "Không có vòng ACTIVE"));
    }
}
