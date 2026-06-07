package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import com.sealhackathon.api.presentation.support.PresentationSlotHelper;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationQueueServiceImpl implements PresentationQueueService {

    private final RoundRepository roundRepository;
    private final HackathonRepository hackathonRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final PresentationSlotRepository presentationSlotRepository;

    @Override
    public PresentationQueueResponse getQueue(Integer roundId) {
        Round round = resolveRound(roundId);
        List<TeamRoundTrack> tracks = teamRoundTrackRepository.findByTrack_Round_Id(round.getId());

        Map<String, List<PresentationQueueResponse.TeamQueueItem>> grouped = new LinkedHashMap<>();
        int total = 0;
        int done = 0;
        int absent = 0;

        for (TeamRoundTrack trt : tracks) {
            if (trt.getParticipationStatus() == ParticipationStatus.ELIMINATED) {
                continue;
            }
            String groupName = trt.getAssignedGroup() != null ? trt.getAssignedGroup() : "Bảng A";
            PresentationSlot slot = presentationSlotRepository
                    .findByRound_IdAndTeam_Id(round.getId(), trt.getTeam().getId())
                    .orElse(null);

            LocalDateTime start = PresentationSlotHelper.resolveStart(trt, slot);
            LocalDateTime end = PresentationSlotHelper.resolveEnd(trt, slot);
            String status = slot != null && slot.getQueueStatus() != null
                    ? slot.getQueueStatus().name()
                    : PresentationQueueStatus.WAITING.name();

            if (PresentationQueueStatus.DONE.name().equals(status)) {
                done++;
            }
            if (PresentationQueueStatus.ELIMINATED.name().equals(status)) {
                absent++;
            }
            total++;

            grouped.computeIfAbsent(groupName, k -> new ArrayList<>()).add(
                    PresentationQueueResponse.TeamQueueItem.builder()
                            .teamId(trt.getTeam().getId())
                            .teamName(trt.getTeam().getTeamName())
                            .order(slot != null ? slot.getSequenceOrder() : trt.getTeam().getId())
                            .status(status)
                            .presentationSchedule(PresentationSlotHelper.formatSchedule(start, end))
                            .location(PresentationSlotHelper.resolveLocation(trt, slot))
                            .build());
        }

        grouped.values().forEach(list ->
                list.sort(Comparator.comparing(PresentationQueueResponse.TeamQueueItem::getOrder,
                        Comparator.nullsLast(Integer::compareTo))));

        List<PresentationQueueResponse.GroupItem> groups = grouped.entrySet().stream()
                .map(e -> PresentationQueueResponse.GroupItem.builder()
                        .groupName(e.getKey())
                        .teams(e.getValue())
                        .build())
                .toList();

        autoStartFirstIfDue(round);

        return PresentationQueueResponse.builder()
                .groups(groups)
                .roomStats(PresentationQueueResponse.RoomStats.builder()
                        .total(total)
                        .done(done)
                        .absent(absent)
                        .build())
                .build();
    }

    @Override
    public PresentationQueueNextResponse advanceNext(Integer roundId, Integer currentTeamId) {
        Round round = resolveRound(roundId);

        List<PresentationSlot> slots = presentationSlotRepository
                .findByRound_IdOrderBySequenceOrderAsc(round.getId());
        if (slots.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Chưa có lịch thuyết trình cho vòng này");
        }

        PresentationSlot presenting = slots.stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.PRESENTING)
                .findFirst()
                .orElse(null);

        if (presenting == null && currentTeamId != null) {
            presenting = slots.stream()
                    .filter(s -> s.getTeam().getId().equals(currentTeamId))
                    .findFirst()
                    .orElse(null);
            if (presenting != null) {
                presenting.setQueueStatus(PresentationQueueStatus.PRESENTING);
                presentationSlotRepository.save(presenting);
            }
        }

        if (presenting != null) {
            presenting.setQueueStatus(PresentationQueueStatus.DONE);
            presentationSlotRepository.save(presenting);
        }

        String groupKey = null;
        if (presenting != null) {
            groupKey = teamRoundTrackRepository.findByTeam_Id(presenting.getTeam().getId()).stream()
                    .filter(trt -> trt.getTrack().getRound().getId().equals(round.getId()))
                    .map(TeamRoundTrack::getAssignedGroup)
                    .findFirst()
                    .orElse(null);
        }

        final String finalGroup = groupKey;
        Optional<PresentationSlot> next = slots.stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.WAITING)
                .filter(s -> finalGroup == null || sameGroup(s, round.getId(), finalGroup))
                .findFirst();

        if (next.isEmpty()) {
            next = slots.stream()
                    .filter(s -> s.getQueueStatus() == PresentationQueueStatus.WAITING)
                    .findFirst();
        }

        if (next.isPresent()) {
            PresentationSlot nextSlot = next.get();
            nextSlot.setQueueStatus(PresentationQueueStatus.PRESENTING);
            presentationSlotRepository.save(nextSlot);
            return PresentationQueueNextResponse.builder()
                    .nextTeamId(nextSlot.getTeam().getId())
                    .build();
        }

        return PresentationQueueNextResponse.builder().nextTeamId(null).build();
    }

    private boolean sameGroup(PresentationSlot slot, Integer roundId, String group) {
        return teamRoundTrackRepository.findByTeam_Id(slot.getTeam().getId()).stream()
                .filter(trt -> trt.getTrack().getRound().getId().equals(roundId))
                .anyMatch(trt -> group.equals(trt.getAssignedGroup()));
    }

    private void autoStartFirstIfDue(Round round) {
        if (round.getExamAt() == null || LocalDateTime.now().isBefore(round.getExamAt())) {
            return;
        }
        boolean hasPresenting = presentationSlotRepository.findByRound_IdOrderBySequenceOrderAsc(round.getId())
                .stream()
                .anyMatch(s -> s.getQueueStatus() == PresentationQueueStatus.PRESENTING);
        if (hasPresenting) {
            return;
        }
        presentationSlotRepository.findByRound_IdOrderBySequenceOrderAsc(round.getId()).stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.WAITING)
                .findFirst()
                .ifPresent(first -> {
                    first.setQueueStatus(PresentationQueueStatus.PRESENTING);
                    presentationSlotRepository.save(first);
                });
    }

    private Round resolveRound(Integer roundId) {
        if (roundId != null) {
            return roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        }
        var ongoing = hackathonRepository.search(
                HackathonStatus.ONGOING, null, null, null, PageRequest.of(0, 1));
        if (!ongoing.hasContent()) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Không có hackathon ONGOING");
        }
        Integer hackathonId = ongoing.getContent().get(0).getId();
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE, "Không có vòng ACTIVE"));
    }
}
