package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.request.PresentationShuffleRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationShuffleResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationTimerBlock;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.support.PresentationNextScoringGuard;
import com.sealhackathon.api.presentation.support.PresentationSlotHelper;
import com.sealhackathon.api.presentation.support.PresentationTimerCalculator;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationQueueServiceImpl implements PresentationQueueService {

    private final RoundRepository roundRepository;
    private final HackathonRepository hackathonRepository;
    private final TrackRepository trackRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final PresentationDurationResolver durationResolver;
    private final PresentationControllerGuard controllerGuard;
    private final AuditService auditService;
    private final PresentationQueuePublisher queuePublisher;
    private final CurrentUserAccessor currentUserAccessor;
    private final PresentationNextScoringGuard nextScoringGuard;

    @Override
    @Transactional(readOnly = true)
    public PresentationQueueResponse getQueue(Integer roundId, Integer trackIdFilter) {
        boolean anonymous = isJudgeAnonymousView();
        Round round = resolveRound(roundId);
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
                if (PresentationQueueStatus.ELIMINATED.name().equals(item.getStatus())) {
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
    public PresentationShuffleResponse shuffle(PresentationShuffleRequest request) {
        Integer roundId = request.getRoundId();
        Round round = resolveRound(roundId);
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Round đã khóa — không thể shuffle queue");
        }

        List<PresentationShuffleResponse.TrackShuffleResult> results = new ArrayList<>();

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            controllerGuard.requireControllerForRound(round.getId(), round);
            int count = shuffleFinalRound(round);
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
            if (presenting.getSubmission() != null) {
                nextScoringGuard.validateBeforeNext(
                        presenting.getSubmission(), trackId, round, acknowledgeIncompleteScoring);
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
        return PresentationQueueResponse.TrackQueueItem.builder()
                .trackId(null)
                .trackName("Chung kết")
                .shuffled(!slots.isEmpty())
                .items(slots.stream().map(slot -> toQueueItem(slot, null, round, anonymous)).toList())
                .build();
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
    }

    private static String defaultLocation(Integer teamId) {
        int room = teamId != null ? (teamId % 3 + 1) : 1;
        return "Online (Teams) - Phòng " + room;
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
