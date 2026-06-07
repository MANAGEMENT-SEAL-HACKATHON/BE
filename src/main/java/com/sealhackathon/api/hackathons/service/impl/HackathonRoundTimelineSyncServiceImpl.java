package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonRoundTimelineSyncService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HackathonRoundTimelineSyncServiceImpl implements HackathonRoundTimelineSyncService {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;

    @Override
    public void syncFromRounds(Integer hackathonId) {
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        if (rounds.isEmpty()) {
            return;
        }

        LocalDate eventStart = rounds.stream()
                .map(r -> r.getExamAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate eventEnd = rounds.stream()
                .map(this::resolveRoundEndDate)
                .max(LocalDate::compareTo)
                .orElse(eventStart);

        Hackathon h = hackathonRepository.findById(hackathonId).orElse(null);
        if (h == null || eventStart == null) {
            return;
        }

        boolean changed = false;
        if (!eventStart.equals(h.getEventStart())) {
            h.setEventStart(eventStart);
            changed = true;
        }
        if (eventEnd != null && !eventEnd.equals(h.getEventEnd())) {
            h.setEventEnd(eventEnd);
            changed = true;
        }
        if (changed) {
            hackathonRepository.save(h);
            log.debug("[TimelineSync] hackathonId={} eventStart={} eventEnd={}", hackathonId, eventStart, eventEnd);
        }
    }

    private LocalDate resolveRoundEndDate(Round round) {
        if (round.getSubmissionDeadline() != null) {
            return round.getSubmissionDeadline().toLocalDate();
        }
        return round.getExamAt().toLocalDate();
    }
}
