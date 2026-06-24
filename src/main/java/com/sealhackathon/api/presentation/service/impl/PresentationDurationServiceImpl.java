package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.presentation.dto.request.PresentationDurationSetupRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationDurationResponse;
import com.sealhackathon.api.presentation.service.PresentationDurationService;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.presentation.support.PresentationDurationMutationGuard;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationDurationServiceImpl implements PresentationDurationService {

    private static final String SCOPE_ROUND = "ROUND";
    private static final String SCOPE_TRACK = "TRACK";

    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final PresentationDurationResolver durationResolver;
    private final PresentationDurationMutationGuard mutationGuard;
    private final PresentationSlotCascadeService slotCascadeService;
    private final HackathonArchiveGuard archiveGuard;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PresentationDurationResponse getDuration(Integer roundId, Integer trackId) {
        Round round = loadRound(roundId);
        if (trackId != null) {
            Track track = loadTrackForRound(trackId, round);
            return toTrackResponse(round, track);
        }
        return toRoundResponse(round);
    }

    @Override
    public PresentationDurationResponse updateDuration(PresentationDurationSetupRequest request) {
        Round round = loadRound(request.getRoundId());
        archiveGuard.assertNotArchivedForRound(round);
        assertDurationMutable(round, request.getTrackId());

        if (request.getTrackId() != null) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                        "Vòng chung kết không dùng track — bỏ trackId",
                        Map.of("roundId", round.getId()));
            }
            Track track = loadTrackForRound(request.getTrackId(), round);
            track.setPresentationMinutes(request.getPresentationMinutes());
            track.setQaMinutes(request.getQaMinutes());
            trackRepository.save(track);
            slotCascadeService.rescheduleForRound(round.getId());
            auditService.log(AuditAction.PRESENTATION_DURATION_UPDATED, "tracks", track.getId(),
                    Map.of(
                            "roundId", round.getId(),
                            "scope", SCOPE_TRACK,
                            "presentationMinutes", request.getPresentationMinutes(),
                            "qaMinutes", request.getQaMinutes()));
            return toTrackResponse(round, track);
        }

        round.setDefaultPresentationMinutes(request.getPresentationMinutes());
        round.setDefaultQaMinutes(request.getQaMinutes());
        roundRepository.save(round);
        slotCascadeService.rescheduleForRound(round.getId());
        auditService.log(AuditAction.PRESENTATION_DURATION_UPDATED, "rounds", round.getId(),
                Map.of(
                        "scope", SCOPE_ROUND,
                        "presentationMinutes", request.getPresentationMinutes(),
                        "qaMinutes", request.getQaMinutes()));
        return toRoundResponse(round);
    }

    @Override
    public PresentationDurationResponse clearTrackOverride(Integer roundId, Integer trackId) {
        Round round = loadRound(roundId);
        archiveGuard.assertNotArchivedForRound(round);
        assertDurationMutable(round, trackId);
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                    "Vòng chung kết không có override track",
                    Map.of("roundId", roundId));
        }
        Track track = loadTrackForRound(trackId, round);
        track.setPresentationMinutes(null);
        track.setQaMinutes(null);
        trackRepository.save(track);
        slotCascadeService.rescheduleForRound(round.getId());
        auditService.log(AuditAction.PRESENTATION_DURATION_UPDATED, "tracks", track.getId(),
                Map.of("roundId", roundId, "scope", SCOPE_TRACK, "clearedOverride", true));
        return toTrackResponse(round, track);
    }

    private void assertDurationMutable(Round round, Integer trackId) {
        mutationGuard.assertMutableBeforePresentation(round, trackId, slotsForScope(round, trackId));
    }

    private List<PresentationSlot> slotsForScope(Round round, Integer trackId) {
        Integer roundId = round.getId();
        if (trackId != null) {
            return presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(roundId, trackId);
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(roundId);
        }
        return presentationSlotRepository.findByRound_IdOrderBySequenceOrderAsc(roundId);
    }

    private Round loadRound(Integer roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
    }

    private Track loadTrackForRound(Integer trackId, Round round) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        if (track.getRound() == null || !track.getRound().getId().equals(round.getId())) {
            throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                    "Track không thuộc round đã chỉ định",
                    Map.of("roundId", round.getId(), "trackId", trackId));
        }
        return track;
    }

    private PresentationDurationResponse toRoundResponse(Round round) {
        return PresentationDurationResponse.builder()
                .roundId(round.getId())
                .scope(SCOPE_ROUND)
                .presentationMinutes(round.getDefaultPresentationMinutes())
                .qaMinutes(round.getDefaultQaMinutes())
                .effectivePresentationMinutes(durationResolver.presentationMinutes(null, round))
                .effectiveQaMinutes(durationResolver.qaMinutes(null, round))
                .build();
    }

    private PresentationDurationResponse toTrackResponse(Round round, Track track) {
        return PresentationDurationResponse.builder()
                .roundId(round.getId())
                .trackId(track.getId())
                .scope(SCOPE_TRACK)
                .presentationMinutes(track.getPresentationMinutes())
                .qaMinutes(track.getQaMinutes())
                .effectivePresentationMinutes(durationResolver.presentationMinutes(track, round))
                .effectiveQaMinutes(durationResolver.qaMinutes(track, round))
                .build();
    }
}
