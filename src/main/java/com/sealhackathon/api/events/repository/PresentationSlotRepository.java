package com.sealhackathon.api.events.repository;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresentationSlotRepository extends JpaRepository<PresentationSlot, Integer> {

    Optional<PresentationSlot> findTopByTeam_IdOrderByStartsAtDesc(Integer teamId);

    Optional<PresentationSlot> findByRound_IdAndTeam_Id(Integer roundId, Integer teamId);

    Optional<PresentationSlot> findByRound_IdAndSubmission_Id(Integer roundId, Integer submissionId);

    List<PresentationSlot> findByRound_IdOrderBySequenceOrderAsc(Integer roundId);

    List<PresentationSlot> findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(Integer roundId, Integer trackId);

    List<PresentationSlot> findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(Integer roundId);

    Optional<PresentationSlot> findFirstByRound_IdAndTrack_IdAndQueueStatus(
            Integer roundId, Integer trackId, PresentationQueueStatus queueStatus);

    Optional<PresentationSlot> findFirstByRound_IdAndTrackIsNullAndQueueStatus(
            Integer roundId, PresentationQueueStatus queueStatus);

    void deleteByRound_IdAndTrack_Id(Integer roundId, Integer trackId);

    void deleteByRound_IdAndTrackIsNull(Integer roundId);
}