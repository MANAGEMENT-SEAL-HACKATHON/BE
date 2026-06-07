package com.sealhackathon.api.events.repository;

import com.sealhackathon.api.events.entity.PresentationSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresentationSlotRepository extends JpaRepository<PresentationSlot, Integer> {

    // Lấy slot thuyết trình mới nhất của một Đội
    Optional<PresentationSlot> findTopByTeam_IdOrderByStartsAtDesc(Integer teamId);
}