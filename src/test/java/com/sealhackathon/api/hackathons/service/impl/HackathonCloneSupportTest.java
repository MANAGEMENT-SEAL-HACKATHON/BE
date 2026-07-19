package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.mapper.CriteriaMapper;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonCloneSupportTest {

    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private CriteriaMapper criteriaMapper;

    @InjectMocks
    private HackathonCloneSupport support;

    @Test
    void copyStructureFrom_clonesRoundsTracksAndCriteria_notOperationalState() {
        Hackathon source = Hackathon.builder().id(1).name("Source").build();
        Hackathon target = Hackathon.builder().id(2).name("Copy").build();

        Round srcPrelim = Round.builder()
                .id(10)
                .hackathon(source)
                .name("Sơ loại")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.now().plusDays(1))
                .submissionDeadline(LocalDateTime.now().plusDays(2))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .isActive(true)
                .problemReleasedAt(LocalDateTime.now())
                .build();
        Round clonedPrelim = Round.builder().id(110).hackathon(target).name("Sơ loại").isFinal(false).build();

        Track srcTrack = Track.builder().id(20).round(srcPrelim).name("Track A").sequenceOrder(1).status(TrackStatus.OPEN).build();
        Track clonedTrack = Track.builder().id(220).round(clonedPrelim).name("Track A").sequenceOrder(1).build();

        Criteria srcCriterion = Criteria.builder().id(30).track(srcTrack).name("Innovation").type(CriteriaType.TECHNICAL).weight(1f).build();
        Criteria clonedCriterion = Criteria.builder().id(330).track(clonedTrack).name("Innovation").type(CriteriaType.TECHNICAL).weight(1f).build();

        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(srcPrelim));
        when(roundRepository.save(any(Round.class))).thenReturn(clonedPrelim);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(srcTrack));
        when(trackRepository.save(any(Track.class))).thenReturn(clonedTrack);
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(20)).thenReturn(List.of(srcCriterion));
        when(criteriaMapper.toCloneForTrack(eq(srcCriterion), eq(clonedTrack), anyInt())).thenReturn(clonedCriterion);

        support.copyStructureFrom(source, target);

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(roundCaptor.capture());
        Round savedRound = roundCaptor.getValue();
        assertThat(savedRound.getIsActive()).isFalse();
        assertThat(savedRound.getProblemReleasedAt()).isNull();
        assertThat(savedRound.getHackathon()).isEqualTo(target);

        verify(criteriaRepository).save(clonedCriterion);
        verify(criteriaMapper).toCloneForTrack(srcCriterion, clonedTrack, 1);
    }
}
