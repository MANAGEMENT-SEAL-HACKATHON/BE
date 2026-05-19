package com.se194093.be.hackathons.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.entity.Event;
import com.se194093.be.events.repository.EventRepository;
import com.se194093.be.events.service.EventScheduleValidator;
import com.se194093.be.events.value_object.EventType;
import com.se194093.be.hackathons.dto.response.HackathonReadinessResponse;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.service.HackathonReadinessService;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.judge_assignments.repository.JudgeAssignmentRepository;
import com.se194093.be.mentor_assignments.repository.MentorAssignmentRepository;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.repository.TrackRepository;
import com.se194093.be.tracks.value_object.TrackStatus;
import com.se194093.be.users.repository.UserRepository;
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
    private final AuditService auditService;

    @Override
    public HackathonReadinessResponse check(Integer hackathonId, HackathonStatus target) {
        HackathonStatus effectiveTarget = (target != null) ? target : HackathonStatus.ONGOING;
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        List<HackathonReadinessResponse.Blocker> blockers = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        if (effectiveTarget == HackathonStatus.ONGOING) {
            checkOngoingReadiness(h, blockers, warnings, summary);
        } else {
            log.info("[Readiness] target={} ngoài scope MF-01 — ready=true (no-op)", effectiveTarget);
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
                totalJudge += judgeAssignmentRepository.findByTrackId(t.getId()).size();
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

        validateKickoffGate(h, hackathonId, blockers);

        summary.put("tracksCount", trackCount);
        summary.put("roundsCount", preliminaryRounds.size() + (int) finalCount);
        summary.put("criteriaCount", totalCriteria);
        summary.put("mentorAssignmentsCount", totalMentor);
        summary.put("judgeAssignmentsCount", totalJudge);
        summary.put("eventsCount", eventRepository.findByHackathonIdOrderByStartsAtAsc(hackathonId).size());
        summary.put("tempJudgesCount", userRepository.searchTempJudges(null, null, PageRequest.of(0, 1))
                .getTotalElements());
    }

    private void validateKickoffGate(Hackathon h, Integer hackathonId,
                                     List<HackathonReadinessResponse.Blocker> blockers) {
        List<Event> kickoffs = eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF);
        if (kickoffs.isEmpty()) {
            blockers.add(blocker(ErrorCode.EVENT_KICKOFF_MISSING,
                    "Hackathon thiếu sự kiện KICKOFF",
                    Map.of("hackathonId", hackathonId)));
            return;
        }
        for (Event kickoff : kickoffs) {
            try {
                eventScheduleValidator.validateBlocking(h, toCreateRequest(kickoff), kickoff.getId());
            } catch (BusinessRuleException ex) {
                blockers.add(blocker(ex.getCode(),
                        "KICKOFF #%d: %s".formatted(kickoff.getId(), ex.getMessage()),
                        Map.of("eventId", kickoff.getId(),
                                "hackathonId", hackathonId,
                                "details", ex.getDetails() == null ? Map.of() : ex.getDetails())));
            }
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
