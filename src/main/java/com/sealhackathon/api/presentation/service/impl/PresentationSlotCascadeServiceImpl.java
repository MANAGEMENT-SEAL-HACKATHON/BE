package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PresentationSlotCascadeServiceImpl implements PresentationSlotCascadeService {

    private final RoundRepository roundRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final PresentationDurationResolver durationResolver;

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

        Map<String, List<PresentationSlot>> groups = new LinkedHashMap<>();
        for (PresentationSlot slot : slots) {
            String key = slot.getTrack() != null ? "track:" + slot.getTrack().getId() : "final";
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
        }

        LocalDateTime examAt = round.getExamAt();
        for (List<PresentationSlot> groupSlots : groups.values()) {
            groupSlots.sort(Comparator.comparing(
                    s -> s.getSequenceOrder() != null ? s.getSequenceOrder() : Integer.MAX_VALUE));
            int order = 1;
            for (PresentationSlot slot : groupSlots) {
                int slotMinutes = durationResolver.slotMinutes(slot.getTrack(), round);
                LocalDateTime startsAt = examAt.plusMinutes((long) (order - 1) * slotMinutes);
                slot.setStartsAt(startsAt);
                slot.setEndsAt(startsAt.plusMinutes(slotMinutes));
                if (slot.getSequenceOrder() == null || slot.getSequenceOrder() <= 0) {
                    slot.setSequenceOrder(order);
                }
                order++;
            }
            presentationSlotRepository.saveAll(groupSlots);
        }
        log.debug("[PresentationCascade] rescheduled {} slots for roundId={}", slots.size(), roundId);
    }
}
