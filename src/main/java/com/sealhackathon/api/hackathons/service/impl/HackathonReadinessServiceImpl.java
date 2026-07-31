package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.EventScheduleValidator;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.dto.response.HackathonReadinessResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonReadinessService;
import com.sealhackathon.api.hackathons.value_object.ReadinessTarget;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.kits.dto.response.KitReconciliationLineResponse;
import com.sealhackathon.api.kits.dto.response.KitReconciliationResponse;
import com.sealhackathon.api.kits.repository.KitBundleRepository;
import com.sealhackathon.api.kits.repository.KitItemRepository;
import com.sealhackathon.api.kits.repository.KitStockRepository;
import com.sealhackathon.api.kits.service.KitService;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HackathonReadinessServiceImpl implements HackathonReadinessService {

    private static final String CODE_READINESS_WARNING = "READINESS_WARNING";

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final EventRepository eventRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final UserRepository userRepository;
    private final WeightSummaryService weightSummaryService;
    private final EventScheduleValidator eventScheduleValidator;
    private final HackathonTimelineService hackathonTimelineService;
    private final AuditService auditService;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final KitItemRepository kitItemRepository;
    private final KitStockRepository kitStockRepository;
    private final KitBundleRepository kitBundleRepository;
    private final KitService kitService;

    @Override
    public HackathonReadinessResponse check(Integer hackathonId, ReadinessTarget target) {
        ReadinessTarget effectiveTarget = (target != null) ? target : ReadinessTarget.ONGOING;
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        List<HackathonReadinessResponse.Blocker> blockers = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        switch (effectiveTarget) {
            case ONGOING -> checkOngoingReadiness(h, blockers, warnings, summary);
            case FINAL_ROUND -> checkFinalRoundReadiness(h, blockers, warnings, summary);
            case AWARDS, PENDING_CONFIRM -> checkAwardsReadiness(h, blockers, warnings, summary);
        }

        boolean ready = blockers.isEmpty();
        auditService.log(AuditAction.HACKATHON_READINESS_CHECK, "hackathons", hackathonId, Map.of(
                "target", effectiveTarget.name(),
                "ready", ready,
                "blockerCount", blockers.size(),
                "warningCount", warnings.size()
        ));

        return HackathonReadinessResponse.builder()
                .ready(ready)
                .targetStatus(effectiveTarget.name())
                .blockers(blockers)
                .warnings(warnings)
                .summary(summary)
                .build();
    }

    private void checkOngoingReadiness(Hackathon h,
                                       List<HackathonReadinessResponse.Blocker> blockers,
                                       List<Warning> warnings,
                                       Map<String, Object> summary) {
        Integer hackathonId = h.getId();

        List<Round> preliminaryRounds = roundRepository.findPreliminaryLikeByHackathonId(hackathonId);
        if (preliminaryRounds.isEmpty()) {
            blockers.add(blocker(ErrorCode.MISSING_PRELIMINARY_ROUND,
                    "Chưa có Vòng Sơ loại (PRELIMINARY/SEMIFINAL)",
                    Map.of("hackathonId", hackathonId)));
        }

        long finalCount = roundRepository.countByHackathon_IdAndIsFinalTrue(hackathonId);
        if (finalCount == 0) {
            blockers.add(blocker(ErrorCode.MISSING_FINAL_ROUND,
                    "Thiếu Round Chung kết (is_final=TRUE)",
                    Map.of("hackathonId", hackathonId)));
        } else if (finalCount > 1) {
            blockers.add(blocker(ErrorCode.MISSING_FINAL_ROUND,
                    "Có %d Round Chung kết — cần đúng 1".formatted(finalCount),
                    Map.of("hackathonId", hackathonId, "finalRoundCount", finalCount)));
        }

        int trackCount = 0;
        long totalCriteria = 0;
        long totalMentor = 0;
        long totalJudge = 0;

        for (Round prelim : preliminaryRounds) {
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                    .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                    .toList();
            if (tracks.isEmpty()) {
                blockers.add(blocker(ErrorCode.MISSING_PRELIMINARY_ROUND,
                        "Round '%s' chưa có Track con".formatted(prelim.getName()),
                        Map.of("roundId", prelim.getId(), "hackathonId", hackathonId)));
            }
            for (Track t : tracks) {
                trackCount++;
                long normal = criteriaRepository.countNormalByTrackId(t.getId());
                totalCriteria += criteriaRepository.countByTrackId(t.getId());
                if (normal == 0) {
                    blockers.add(blocker(ErrorCode.ROUND_NO_CRITERIA,
                            "Track '%s' chưa có Criteria".formatted(t.getName()),
                            Map.of("trackId", t.getId(), "roundId", prelim.getId())));
                } else if (!weightSummaryService.isValidForTrack(t.getId())) {
                    double raw = weightSummaryService.rawTotalForTrack(t.getId()).orElse(0.0);
                    blockers.add(blocker(ErrorCode.TRACK_CRITERIA_WEIGHT,
                            "Track '%s': tổng weight = %.4f, cần 1.0".formatted(t.getName(), raw),
                            Map.of("trackId", t.getId(), "total", raw)));
                }
                int mentors = mentorAssignmentRepository.findByTrackId(t.getId()).size();
                totalMentor += mentors;
                if (mentors == 0) {
                    warnings.add(Warning.of(CODE_READINESS_WARNING,
                            "Track '%s' chưa có Mentor".formatted(t.getName()),
                            Map.of("trackId", t.getId())));
                }
                int judges = judgeAssignmentRepository.findByTrackId(t.getId()).size();
                totalJudge += judges;
                if (judges == 0) {
                    warnings.add(Warning.of(CODE_READINESS_WARNING,
                            "Track '%s' chưa có Judge NORMAL".formatted(t.getName()),
                            Map.of("trackId", t.getId())));
                }
            }
        }

        Optional<Round> finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId);
        if (finalRound.isPresent()) {
            Round fr = finalRound.get();
            long normalFinal = criteriaRepository.countNormalByFinalRoundId(fr.getId());
            totalCriteria += criteriaRepository.countNormalByFinalRoundId(fr.getId());
            if (normalFinal == 0) {
                blockers.add(blocker(ErrorCode.ROUND_NO_CRITERIA,
                        "Round Chung kết chưa có Criteria",
                        Map.of("roundId", fr.getId())));
            } else if (!weightSummaryService.isValidForFinalRound(fr.getId())) {
                double raw = weightSummaryService.rawTotalForFinalRound(fr.getId()).orElse(0.0);
                blockers.add(blocker(ErrorCode.FINAL_CRITERIA_WEIGHT,
                        "Round Chung kết: tổng weight = %.4f, cần 1.0".formatted(raw),
                        Map.of("roundId", fr.getId(), "total", raw)));
            }
        }

        validateMilestoneEventsGate(h, hackathonId, blockers);
        validateRoundsExamAtGate(hackathonId, blockers);
        addKitInventoryWarnings(hackathonId, warnings, summary);

        summary.put("tracksCount", trackCount);
        summary.put("roundsCount", preliminaryRounds.size() + (int) finalCount);
        summary.put("criteriaCount", totalCriteria);
        summary.put("mentorAssignmentsCount", totalMentor);
        summary.put("judgeAssignmentsCount", totalJudge);
        summary.put("eventsCount", eventRepository.findByHackathonIdOrderByStartsAtAsc(hackathonId).size());
        summary.put("tempJudgesCount", userRepository.searchTempJudges(null, null, PageRequest.of(0, 1))
                .getTotalElements());
    }

    private void checkFinalRoundReadiness(Hackathon h,
                                          List<HackathonReadinessResponse.Blocker> blockers,
                                          List<Warning> warnings,
                                          Map<String, Object> summary) {
        Integer hackathonId = h.getId();
        Optional<Round> finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId);
        if (finalRound.isEmpty()) {
            blockers.add(blocker(ErrorCode.MISSING_FINAL_ROUND,
                    "Thiếu Round Chung kết (is_final=TRUE)",
                    Map.of("hackathonId", hackathonId)));
            return;
        }

        Round fr = finalRound.get();
        long normalFinal = criteriaRepository.countNormalByFinalRoundId(fr.getId());
        if (normalFinal == 0) {
            blockers.add(blocker(ErrorCode.ROUND_NO_CRITERIA,
                    "Round Chung kết chưa có Criteria",
                    Map.of("roundId", fr.getId())));
        } else if (!weightSummaryService.isValidForFinalRound(fr.getId())) {
            double raw = weightSummaryService.rawTotalForFinalRound(fr.getId()).orElse(0.0);
            blockers.add(blocker(ErrorCode.FINAL_CRITERIA_WEIGHT,
                    "Round Chung kết: tổng weight = %.4f, cần 1.0".formatted(raw),
                    Map.of("roundId", fr.getId(), "total", raw)));
        }

        long finalJudges = judgeAssignmentRepository.findByRoundId(fr.getId()).stream()
                .filter(ja -> ja.getAssignmentType() == JudgeAssignmentType.FINAL_EXTERNAL)
                .count();
        if (finalJudges == 0) {
            blockers.add(blocker(ErrorCode.JUDGE_FINAL_AT_PHASE1,
                    "Chưa phân Judge Chung kết (FINAL_EXTERNAL)",
                    Map.of("roundId", fr.getId(), "hackathonId", hackathonId)));
        }

        long teamCount = teamRoundParticipationRepository.countByRound_Id(fr.getId());
        if (teamCount == 0) {
            blockers.add(blocker(ErrorCode.NO_TEAMS_IN_ROUND,
                    "Chưa có đội tham gia Round Chung kết — cần advance từ GĐ4",
                    Map.of("roundId", fr.getId(), "hackathonId", hackathonId)));
        }

        if (!eventRepository.existsByHackathonIdAndType(hackathonId, EventType.AWARDS)) {
            warnings.add(Warning.of(CODE_READINESS_WARNING,
                    "Chưa có sự kiện AWARDS (lễ trao giải) — khuyến nghị trước GĐ6",
                    Map.of("hackathonId", hackathonId)));
        }

        summary.put("finalRoundId", fr.getId());
        summary.put("finalExternalJudgeCount", finalJudges);
        summary.put("finalTeamCount", teamCount);
    }

    private void checkAwardsReadiness(Hackathon h,
                                      List<HackathonReadinessResponse.Blocker> blockers,
                                      List<Warning> warnings,
                                      Map<String, Object> summary) {
        Integer hackathonId = h.getId();
        if (!eventRepository.existsByHackathonIdAndType(hackathonId, EventType.AWARDS)) {
            blockers.add(blocker(ErrorCode.EVENT_AWARDS_MISSING,
                    "Thiếu sự kiện AWARDS (lễ trao giải)",
                    Map.of("hackathonId", hackathonId)));
        } else {
            for (Event event : eventRepository.findByHackathonIdAndType(hackathonId, EventType.AWARDS)) {
                try {
                    eventScheduleValidator.validateBlocking(h, toCreateRequest(event), event.getId());
                } catch (BusinessRuleException ex) {
                    blockers.add(blocker(ex.getCode(),
                            "AWARDS #%d: %s".formatted(event.getId(), ex.getMessage()),
                            Map.of("eventId", event.getId(),
                                    "hackathonId", hackathonId,
                                    "details", ex.getDetails() == null ? Map.of() : ex.getDetails())));
                }
            }
        }
        summary.put("eventsCount", eventRepository.findByHackathonIdOrderByStartsAtAsc(hackathonId).size());
    }

    private void validateMilestoneEventsGate(Hackathon h, Integer hackathonId,
                                             List<HackathonReadinessResponse.Blocker> blockers) {
        if (!eventRepository.existsByHackathonIdAndType(hackathonId, EventType.KICKOFF)) {
            blockers.add(blocker(ErrorCode.EVENT_KICKOFF_MISSING,
                    "Hackathon thiếu sự kiện KICKOFF",
                    Map.of("hackathonId", hackathonId)));
        }
        for (EventType type : EventTimeline.MILESTONE_TYPES) {
            for (Event event : eventRepository.findByHackathonIdAndType(hackathonId, type)) {
                try {
                    eventScheduleValidator.validateBlocking(h, toCreateRequest(event), event.getId());
                } catch (BusinessRuleException ex) {
                    blockers.add(blocker(ex.getCode(),
                            "%s #%d: %s".formatted(type.name(), event.getId(), ex.getMessage()),
                            Map.of("eventId", event.getId(),
                                    "eventType", type.name(),
                                    "hackathonId", hackathonId,
                                    "details", ex.getDetails() == null ? Map.of() : ex.getDetails())));
                }
            }
        }
    }

    private void validateRoundsExamAtGate(Integer hackathonId,
                                          List<HackathonReadinessResponse.Blocker> blockers) {
        for (BusinessRuleException ex : hackathonTimelineService.collectRoundExamAtViolations(hackathonId)) {
            String code = ex.getCode();
            if (ErrorCode.EVENT_PRESENTATION_MISSING.equals(code)
                    || ErrorCode.EVENT_AWARDS_MISSING.equals(code)) {
                continue;
            }
            blockers.add(blocker(code, ex.getMessage(),
                    ex.getDetails() == null ? Map.of("hackathonId", hackathonId) : ex.getDetails()));
        }
    }

    private void addKitInventoryWarnings(Integer hackathonId, List<Warning> warnings,
                                         Map<String, Object> summary) {
        boolean hasKickoff = eventRepository.existsByHackathonIdAndType(hackathonId, EventType.KICKOFF);
        long itemCount = kitItemRepository.countByHackathon_Id(hackathonId);
        summary.put("kitItemCount", itemCount);

        if (hasKickoff && itemCount == 0) {
            warnings.add(Warning.of(CODE_READINESS_WARNING,
                    "Khuyến nghị — chưa khai món kit (áo/dây đeo) trước Kickoff. Không chặn kích hoạt.",
                    Map.of("hackathonId", hackathonId, "kitHint", "NO_ITEMS")));
            return;
        }
        if (itemCount == 0) {
            return;
        }

        var items = kitItemRepository.findByHackathon_IdOrderByIdAsc(hackathonId);
        var stocks = kitStockRepository.findByKitItem_IdIn(items.stream().map(i -> i.getId()).toList());
        int totalQty = stocks.stream()
                .mapToInt(s -> s.getQuantityTotal() == null ? 0 : s.getQuantityTotal())
                .sum();
        summary.put("kitStockTotal", totalQty);
        if (totalQty == 0) {
            warnings.add(Warning.of(CODE_READINESS_WARNING,
                    "Khuyến nghị — đã có món kit nhưng tổng tồn kho = 0. Không chặn kích hoạt.",
                    Map.of("hackathonId", hackathonId, "kitHint", "ZERO_STOCK")));
        }

        boolean hasDefaultBundle = !kitBundleRepository.findByHackathon_IdAndIsDefaultTrue(hackathonId).isEmpty();
        summary.put("kitHasDefaultBundle", hasDefaultBundle);
        if (!hasDefaultBundle) {
            warnings.add(Warning.of(CODE_READINESS_WARNING,
                    "Khuyến nghị — chưa có combo kit mặc định để phát nhanh tại quầy. Không chặn kích hoạt.",
                    Map.of("hackathonId", hackathonId, "kitHint", "NO_DEFAULT_BUNDLE")));
        }

        try {
            KitReconciliationResponse recon = kitService.reconciliation(hackathonId);
            int shortfall = 0;
            if (recon.getLines() != null) {
                for (KitReconciliationLineResponse line : recon.getLines()) {
                    if (line.getShortfall() != null && line.getShortfall() > 0) {
                        shortfall += line.getShortfall();
                    }
                }
            }
            summary.put("kitShortfallTotal", shortfall);
            if (shortfall > 0) {
                warnings.add(Warning.of(CODE_READINESS_WARNING,
                        "Khuyến nghị — tồn kho thiếu %d đơn vị so với nhu cầu (size/dáng). Không chặn kích hoạt."
                                .formatted(shortfall),
                        Map.of("hackathonId", hackathonId, "kitHint", "SHORTFALL", "shortfall", shortfall)));
            }
        } catch (Exception ex) {
            log.debug("Kit reconciliation skipped in readiness: {}", ex.getMessage());
        }
    }

    private static CreateEventRequest toCreateRequest(Event e) {
        return CreateEventRequest.builder()
                .title(e.getTitle())
                .type(e.getType())
                .description(e.getDescription())
                .location(e.getLocation())
                .meetUrl(e.getMeetUrl())
                .startsAt(e.getStartsAt())
                .endsAt(e.getEndsAt())
                .isPublic(e.getIsPublic())
                .build();
    }

    private static HackathonReadinessResponse.Blocker blocker(String code, String message,
                                                              Map<String, Object> details) {
        return HackathonReadinessResponse.Blocker.builder()
                .code(code).message(message).details(details)
                .build();
    }
}
