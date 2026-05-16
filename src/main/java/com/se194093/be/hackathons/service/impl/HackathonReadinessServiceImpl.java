package com.se194093.be.hackathons.service.impl;

import com.se194093.be.hackathons.dto.response.HackathonReadinessResponse;
import com.se194093.be.hackathons.service.HackathonReadinessService;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-06-status.md}.
 *
 * <p>Inject (rất nhiều — đây là service tổng hợp):
 * <ul>
 *   <li>HackathonRepository, TrackRepository, RoundRepository, CriteriaRepository,
 *       EventRepository, MentorAssignmentRepository, JudgeAssignmentRepository, UserRepository</li>
 *   <li>WeightSummaryService (helper isValid + warningIfNotOne)</li>
 *   <li>AuditService</li>
 * </ul>
 *
 * <p>Logic dài — nên tách helper {@code checkOngoingReadiness(...)} cho rõ ràng và dễ test.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HackathonReadinessServiceImpl implements HackathonReadinessService {

    @Override
    public HackathonReadinessResponse check(Integer hackathonId, HackathonStatus target) {
        // TODO Dev:
        //  target = target != null ? target : ONGOING
        //  h = hackathonRepo.findById(hackathonId) or 404
        //  blockers = []; warnings = []
        //  summary = new HashMap<>();
        //  if target == ONGOING:
        //     // Rule 1 — ≥ 1 Track
        //     tracks = trackRepo.findByHackathonIdOrderById(hackathonId)
        //     summary.put("tracksCount", tracks.size())
        //     if tracks.isEmpty(): blockers.add(MISSING_TRACK,"Chưa có Track")
        //     // Rule 2 — mỗi Track ≥ 2 Round
        //     totalRounds = 0; totalCriteria = 0
        //     for t in tracks:
        //         rounds = roundRepo.findByTrackIdOrderBySequenceOrderAsc(t.id)
        //         totalRounds += rounds.size()
        //         if rounds.size() < 2:
        //             blockers.add(ROUND_COUNT_INSUFFICIENT, "Track {t.name} có {rounds.size()} round, cần ≥ 2",
        //                           {trackId: t.id, current: rounds.size()})
        //         for r in rounds:
        //             cnt = criteriaRepo.countNormalByRoundId(r.id)
        //             totalCriteria += criteriaRepo.countByRoundId(r.id)
        //             if cnt == 0:
        //                 blockers.add(ROUND_NO_CRITERIA, "Round {r.name} chưa có Criteria",
        //                              {roundId: r.id, trackId: t.id})
        //             else if !weightSummaryService.isValid(r.id):
        //                 total = weightSummaryService.rawTotal(r.id).orElse(0.0)
        //                 blockers.add(ROUND_WEIGHT_NOT_ONE,
        //                     "Track {t.name} - Round {r.name}: tổng weight {total}",
        //                     {trackId: t.id, roundId: r.id, total: total})
        //     summary.put("roundsCount", totalRounds)
        //     summary.put("criteriaCount", totalCriteria)
        //     // Rule 3 — ≥ 1 KICKOFF
        //     if !eventRepo.existsByHackathonIdAndType(hackathonId, KICKOFF):
        //         blockers.add(EVENT_KICKOFF_MISSING, "Thiếu sự kiện KICKOFF")
        //     summary.put("eventsCount", eventRepo.findByHackathonIdOrderByStartsAtAsc(hackathonId).size())
        //     // Soft warnings
        //     for t in tracks:
        //         if mentorAssignmentRepo.findByTrackId(t.id).isEmpty():
        //             warnings.add(Warning.of("READINESS_WARNING", "Track {t.name} chưa có Mentor",
        //                                     {trackId: t.id}))
        //     summary.put("mentorAssignmentsCount", ...)
        //     summary.put("judgeAssignmentsCount", ...)
        //     summary.put("tempJudgesCount", ...)
        //
        //  ready = blockers.isEmpty()
        //  audit.log(HACKATHON_READINESS_CHECK, "hackathons", hackathonId,
        //            {target, ready, blockerCount: blockers.size(), warningCount: warnings.size()})
        //  return HackathonReadinessResponse.builder()
        //                                   .ready(ready).targetStatus(target.name())
        //                                   .blockers(blockers).warnings(warnings).summary(summary)
        //                                   .build()
        throw new UnsupportedOperationException("FR-06 readiness check - to be implemented");
    }
}
