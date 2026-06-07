package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PresentationSlotCascadeServiceImpl implements PresentationSlotCascadeService {

    private static final int SLOT_MINUTES = 15;

    private final RoundRepository roundRepository;
    private final PresentationSlotRepository presentationSlotRepository;

    @Override
    public void rescheduleForRound(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (Boolean.TRUE.equals(round.getScoringLocked()) || round.getExamAt() == null) {
            return;
        }

        List<PresentationSlot> slots = presentationSlotRepository.findByRound_IdOrderBySequenceOrderAsc(roundId);
        if (slots.isEmpty()) {
            return;
        }
        boolean hasDone = slots.stream()
                .anyMatch(s -> s.getQueueStatus() == PresentationQueueStatus.DONE);
        if (hasDone) {
            log.debug("[PresentationCascade] skip roundId={} — có slot DONE", roundId);
            return;
        }

        LocalDateTime examAt = round.getExamAt();
        for (PresentationSlot slot : slots) {
            int seq = slot.getSequenceOrder() != null && slot.getSequenceOrder() > 0
                    ? slot.getSequenceOrder()
                    : 1;
            LocalDateTime startsAt = examAt.plusMinutes((long) (seq - 1) * SLOT_MINUTES);
            slot.setStartsAt(startsAt);
            slot.setEndsAt(startsAt.plusMinutes(SLOT_MINUTES));
        }
        presentationSlotRepository.saveAll(slots);
        log.debug("[PresentationCascade] rescheduled {} slots for roundId={}", slots.size(), roundId);
    }
}
