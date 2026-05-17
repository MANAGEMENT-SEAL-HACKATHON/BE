package com.se194093.be.hackathons.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.service.WeightSummaryService;
import com.se194093.be.events.repository.EventRepository;
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

/**
 * FR-06 readiness — tổng hợp blockers / warnings / summary cho 1 transition đích.
 *
 * <p>Hiện chỉ implement đầy đủ rule cho target {@link HackathonStatus#ONGOING}. Các target khác
 * (PENDING_CONFIRM, FINISHED) sẽ thêm rule sau ở GĐ5/GĐ6 — tạm thời trả ready=true (no-op).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HackathonReadinessServiceImpl implements HackathonReadinessService {

    private static final String CODE_MISSING_TRACK             = "MISSING_TRACK";
    private static final String CODE_ROUND_COUNT_INSUFFICIENT  = "ROUND_COUNT_INSUFFICIENT";
    private static final String CODE_ROUND_NO_CRITERIA         = "ROUND_NO_CRITERIA";
    private static final String CODE_ROUND_WEIGHT_NOT_ONE      = "ROUND_WEIGHT_NOT_ONE";
    private static final String CODE_EVENT_KICKOFF_MISSING     = "EVENT_KICKOFF_MISSING";
    private static final String CODE_READINESS_WARNING         = "READINESS_WARNING";

    private final HackathonRepository hackathonRepository;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final CriteriaRepository criteriaRepository;
    private final EventRepository eventRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final UserRepository userRepository;
    private final WeightSummaryService weightSummaryService;
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
            log.info("[Readiness] target={} ngoài scope MF-01 — tạm thời ready=true (no-op)", effectiveTarget);
        }

        boolean ready = blockers.isEmpty();

        auditService.log(AuditAction.HACKATHON_READINESS_CHECK, "hackathons", hackathonId, Map.of(
                "target",       effectiveTarget.name(),
                "ready",        ready,
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

        // Rule 1 — ≥ 1 Track
        List<Track> tracks = trackRepository.findByHackathonIdOrderById(hackathonId);
        summary.put("tracksCount", tracks.size());
        if (tracks.isEmpty()) {
            blockers.add(blocker(CODE_MISSING_TRACK,
                    "Hackathon chưa có Track nào",
                    Map.of("hackathonId", hackathonId)));
        }

        // Rule 2 — mỗi Track: rounds, criteria, weight
        int totalRounds = 0;
        long totalCriteria = 0;
        long totalMentorAssign = 0;
        long totalJudgeAssign = 0;
        for (Track t : tracks) {
            List<Round> rounds = roundRepository.findByTrackIdOrderBySequenceOrderAsc(t.getId());
            totalRounds += rounds.size();

            if (rounds.size() < 1) {
                blockers.add(blocker(CODE_ROUND_COUNT_INSUFFICIENT,
                        "Track '%s' có %d round, cần ≥ 1".formatted(t.getName(), rounds.size()),
                        Map.of("trackId", t.getId(), "current", rounds.size())));
            }

            for (Round r : rounds) {
                long normalCount = criteriaRepository.countNormalByRoundId(r.getId());
                totalCriteria += criteriaRepository.countByRoundId(r.getId());
                if (normalCount == 0) {
                    blockers.add(blocker(CODE_ROUND_NO_CRITERIA,
                            "Round '%s' (Track '%s') chưa có Criteria"
                                    .formatted(r.getName(), t.getName()),
                            Map.of("roundId", r.getId(), "trackId", t.getId())));
                } else if (!weightSummaryService.isValid(r.getId())) {
                    double rawTotal = weightSummaryService.rawTotal(r.getId()).orElse(0.0);
                    blockers.add(blocker(CODE_ROUND_WEIGHT_NOT_ONE,
                            "Round '%s' (Track '%s'): tổng weight = %.4f, cần 1.0"
                                    .formatted(r.getName(), t.getName(), rawTotal),
                            Map.of("roundId", r.getId(), "trackId", t.getId(), "total", rawTotal)));
                }
            }

            // Soft warning — mentor missing on track
            int mentorOnTrack = mentorAssignmentRepository.findByTrackId(t.getId()).size();
            totalMentorAssign += mentorOnTrack;
            if (mentorOnTrack == 0) {
                warnings.add(Warning.of(CODE_READINESS_WARNING,
                        "Track '%s' chưa có Mentor".formatted(t.getName()),
                        Map.of("trackId", t.getId())));
            }

            for (Round r : rounds) {
                totalJudgeAssign += judgeAssignmentRepository.findByRoundId(r.getId()).size();
            }
        }
        summary.put("roundsCount", totalRounds);
        summary.put("criteriaCount", totalCriteria);
        summary.put("mentorAssignmentsCount", totalMentorAssign);
        summary.put("judgeAssignmentsCount", totalJudgeAssign);

        // Rule 3 — ≥ 1 KICKOFF event
        boolean hasKickoff = eventRepository.existsByHackathonIdAndType(hackathonId, EventType.KICKOFF);
        if (!hasKickoff) {
            blockers.add(blocker(CODE_EVENT_KICKOFF_MISSING,
                    "Hackathon thiếu sự kiện KICKOFF",
                    Map.of("hackathonId", hackathonId)));
        }
        summary.put("eventsCount", eventRepository.findByHackathonIdOrderByStartsAtAsc(hackathonId).size());

        long tempJudgeCount = userRepository.searchTempJudges(null, null, PageRequest.of(0, 1))
                .getTotalElements();
        summary.put("tempJudgesCount", tempJudgeCount);
    }

    private static HackathonReadinessResponse.Blocker blocker(String code, String message,
                                                              Map<String, Object> details) {
        return HackathonReadinessResponse.Blocker.builder()
                .code(code).message(message).details(details)
                .build();
    }
}
