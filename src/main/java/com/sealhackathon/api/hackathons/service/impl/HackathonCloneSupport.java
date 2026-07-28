package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.mapper.CriteriaMapper;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sao chép cấu trúc rounds + tracks + criteria khi nhân bản hackathon (Đáp án 3).
 */
@Component
@RequiredArgsConstructor
public class HackathonCloneSupport {

    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final CriteriaMapper criteriaMapper;

    public void copyStructureFrom(Hackathon source, Hackathon target) {
        List<Round> sourceRounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(source.getId());
        Map<Integer, Round> roundMap = new HashMap<>();

        for (Round srcRound : sourceRounds) {
            Round clonedRound = roundRepository.save(copyRound(srcRound, target));
            roundMap.put(srcRound.getId(), clonedRound);

            if (Boolean.TRUE.equals(srcRound.getIsFinal())) {
                cloneFinalRoundCriteria(srcRound, clonedRound);
            } else {
                cloneTracksAndCriteria(srcRound, clonedRound);
            }
        }
    }

    private static Round copyRound(Round src, Hackathon targetHackathon) {
        // Structure only: do not copy exam/submission schedule — new DRAFT must re-enter dates
        // (copied past deadlines would make FE treat rounds as "Đã kết thúc" and hide Edit).
        return Round.builder()
                .hackathon(targetHackathon)
                .name(src.getName())
                .examAt(null)
                .isFinal(src.getIsFinal())
                .roundType(src.getRoundType())
                .codingDurationHours(src.getCodingDurationHours())
                .submissionOpen(null)
                .submissionDeadline(null)
                .lateSubmissionPolicy(src.getLateSubmissionPolicy())
                .topNAdvance(src.getTopNAdvance())
                .minTeamsFinal(src.getMinTeamsFinal())
                .wildcardEnabled(src.getWildcardEnabled())
                .tiebreakRule(src.getTiebreakRule())
                .defaultPresentationMinutes(src.getDefaultPresentationMinutes())
                .defaultQaMinutes(src.getDefaultQaMinutes())
                .isActive(false)
                .scoringLocked(false)
                .forceLocked(false)
                .isPublished(false)
                .presentationShuffled(false)
                .build();
    }

    private void cloneTracksAndCriteria(Round srcRound, Round targetRound) {
        List<Track> sourceTracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(srcRound.getId());
        for (Track srcTrack : sourceTracks) {
            Track clonedTrack = trackRepository.save(copyTrack(srcTrack, targetRound));
            List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(srcTrack.getId());
            int displayOrder = 1;
            for (Criteria srcCriterion : criteria) {
                criteriaRepository.save(criteriaMapper.toCloneForTrack(srcCriterion, clonedTrack, displayOrder++));
            }
        }
    }

    private void cloneFinalRoundCriteria(Round srcRound, Round targetRound) {
        List<Criteria> criteria = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(srcRound.getId());
        int displayOrder = 1;
        for (Criteria srcCriterion : criteria) {
            criteriaRepository.save(criteriaMapper.toCloneForFinalRound(srcCriterion, targetRound, displayOrder++));
        }
    }

    private static Track copyTrack(Track src, Round targetRound) {
        return Track.builder()
                .round(targetRound)
                .name(src.getName())
                .description(src.getDescription())
                .maxTeams(src.getMaxTeams())
                .maxTeamsPerGroup(src.getMaxTeamsPerGroup())
                .minTeamSize(src.getMinTeamSize())
                .maxTeamSize(src.getMaxTeamSize())
                .status(TrackStatus.OPEN)
                .sequenceOrder(src.getSequenceOrder())
                .presentationMinutes(src.getPresentationMinutes())
                .qaMinutes(src.getQaMinutes())
                .presentationShuffled(false)
                .build();
    }
}
