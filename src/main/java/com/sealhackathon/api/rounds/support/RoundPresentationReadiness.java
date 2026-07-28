package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Shared Gate 2–3 readiness for lockScoring and Round DTO flags.
 */
@Component
@RequiredArgsConstructor
public class RoundPresentationReadiness {

    private static final Set<PresentationQueueStatus> TERMINAL = EnumSet.of(
            PresentationQueueStatus.DONE,
            PresentationQueueStatus.ELIMINATED);

    private final TrackRepository trackRepository;
    private final PresentationSlotRepository presentationSlotRepository;

    @Getter
    @Builder
    public static class Flags {
        private final boolean presentationShuffled;
        private final boolean presentationsComplete;
    }

    public Flags evaluate(Round round) {
        if (round == null || round.getId() == null) {
            return Flags.builder().presentationShuffled(false).presentationsComplete(false).build();
        }
        boolean shuffled = isShuffled(round);
        boolean complete = shuffled && arePresentationsComplete(round);
        return Flags.builder()
                .presentationShuffled(shuffled)
                .presentationsComplete(complete)
                .build();
    }

    public void assertShuffled(Round round) {
        if (!isShuffled(round)) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED,
                    "Chưa xáo trộn hàng đợi thuyết trình, không thể khóa chấm!");
        }
    }

    public void assertPresentationsComplete(Round round) {
        if (!arePresentationsComplete(round)) {
            throw new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_PRESENTATIONS_INCOMPLETE,
                    "Chưa hoàn tất thuyết trình (còn đội WAITING/PRESENTING), không thể khóa chấm!");
        }
    }

    public boolean isShuffled(Round round) {
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return Boolean.TRUE.equals(round.getPresentationShuffled());
        }
        List<Track> tracks = activeTracks(round.getId());
        if (tracks.isEmpty()) {
            return false;
        }
        return tracks.stream().allMatch(t -> Boolean.TRUE.equals(t.getPresentationShuffled()));
    }

    public boolean arePresentationsComplete(Round round) {
        if (!isShuffled(round)) {
            return false;
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return slotsComplete(presentationSlotRepository
                    .findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(round.getId()));
        }
        for (Track track : activeTracks(round.getId())) {
            List<PresentationSlot> slots = presentationSlotRepository
                    .findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(round.getId(), track.getId());
            if (!slotsComplete(slots)) {
                return false;
            }
        }
        return true;
    }

    private List<Track> activeTracks(Integer roundId) {
        return trackRepository.findByRoundIdOrderBySequenceOrderAsc(roundId).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();
    }

    /** 0 slots (after shuffle with 0 gradable) = complete. */
    private static boolean slotsComplete(List<PresentationSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return true;
        }
        return slots.stream().allMatch(s -> {
            PresentationQueueStatus status = s.getQueueStatus();
            return status != null && TERMINAL.contains(status);
        });
    }
}
