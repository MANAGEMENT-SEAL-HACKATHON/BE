package com.sealhackathon.api.events.repository;

import com.sealhackathon.api.events.entity.PresentationSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresentationSlotRepository extends JpaRepository<PresentationSlot, Integer> {

    Optional<PresentationSlot> findTopByTeam_IdOrderByStartsAtDesc(Integer teamId);

    Optional<PresentationSlot> findByRound_IdAndTeam_Id(Integer roundId, Integer teamId);

    List<PresentationSlot> findByRound_IdOrderBySequenceOrderAsc(Integer roundId);
}