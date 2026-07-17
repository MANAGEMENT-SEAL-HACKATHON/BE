package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.WildcardCandidateSelection;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.service.TeamDqBackfillService;
import com.sealhackathon.api.teams.service.TeamDqBackfillService.AdvancedPrelimSeat;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.wildcard_reviews.entity.WildcardReview;
import com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TeamDqBackfillServiceImpl implements TeamDqBackfillService {

    private final RoundRepository roundRepository;
    private final RoundRankingQueryService roundRankingQueryService;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final WildcardReviewRepository wildcardReviewRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    public void afterEliminate(Team eliminatedTeam,
                               List<AdvancedPrelimSeat> previouslyAdvancedPrelimSeats,
                               String reason) {
        notifyTeam(eliminatedTeam,
                "TEAM_DQ",
                "Đội bị loại khỏi cuộc thi",
                "Đội \"" + eliminatedTeam.getTeamName() + "\" đã bị loại. Lý do: "
                        + (reason != null ? reason.trim() : ""),
                eliminatedTeam.getId());

        if (previouslyAdvancedPrelimSeats == null || previouslyAdvancedPrelimSeats.isEmpty()) {
            return;
        }

        Hackathon hackathon = eliminatedTeam.getHackathon();
        Optional<Round> finalRoundOpt = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathon.getId());

        for (AdvancedPrelimSeat seat : previouslyAdvancedPrelimSeats) {
            TeamRoundTrack advancedTrt = seat.trt();
            Track track = advancedTrt.getTrack();
            Round prelim = track != null ? track.getRound() : null;
            if (prelim == null || Boolean.TRUE.equals(prelim.getIsFinal())) {
                continue;
            }

            removeFinalParticipation(eliminatedTeam, finalRoundOpt.orElse(null));

            if (hackathon.getStatus() == HackathonStatus.FINISHED) {
                auditService.log(AuditAction.DQ_AFTER_FINISHED_LOG_ONLY, "teams", eliminatedTeam.getId(),
                        baseAudit(eliminatedTeam, prelim, track, reason, null));
                continue;
            }

            if (finalRoundOpt.isPresent() && Boolean.TRUE.equals(finalRoundOpt.get().getIsActive())) {
                auditService.log(AuditAction.DQ_REJECTED_CK_ACTIVE, "teams", eliminatedTeam.getId(),
                        baseAudit(eliminatedTeam, prelim, track, reason, null));
                continue;
            }

            if (seat.wasWildcard()) {
                backfillFromWildcardPool(eliminatedTeam, prelim, track, finalRoundOpt.orElse(null), reason);
            } else {
                backfillFromTopNBench(eliminatedTeam, prelim, track, advancedTrt.getAssignedGroup(),
                        finalRoundOpt.orElse(null), reason);
            }
        }
    }

    private void backfillFromTopNBench(Team dqTeam, Round prelim, Track track, String assignedGroup,
                                       Round finalRound, String reason) {
        List<RoundRankingItemResponse> ranking =
                roundRankingQueryService.rankingForRound(prelim.getId(), false);

        // After advance, bench teams are TRT=ELIMINATED but Team still ACTIVE — eligible to bubble up.
        // Exclude only DQ'd teams (TeamStatus.ELIMINATED) and those already ADVANCED.
        Optional<RoundRankingItemResponse> candidateOpt = ranking.stream()
                .filter(r -> Objects.equals(r.getTrackId(), track.getId()))
                .filter(r -> sameGroup(assignedGroup, r.getAssignedGroup()))
                .filter(r -> !ParticipationStatus.ADVANCED.name().equals(r.getParticipationStatus()))
                .filter(r -> !Objects.equals(r.getTeamId(), dqTeam.getId()))
                .filter(r -> isTeamEligibleForBackfill(r.getTeamId()))
                .min(Comparator.comparing(RoundRankingItemResponse::getRank,
                        Comparator.nullsLast(Integer::compareTo)));

        if (candidateOpt.isEmpty()) {
            auditService.log(AuditAction.DQ_NO_BACKFILL_BENCH_EMPTY, "teams", dqTeam.getId(),
                    baseAudit(dqTeam, prelim, track, reason, null));
            return;
        }

        promoteCandidate(candidateOpt.get().getTeamId(), prelim, track, finalRound, dqTeam, reason,
                AuditAction.TOP_N_BACKFILL, false);
    }

    private void backfillFromWildcardPool(Team dqTeam, Round prelim, Track track,
                                          Round finalRound, String reason) {
        Integer topN = prelim.getTopNAdvance();
        int topNVal = topN != null && topN > 0 ? topN : 0;

        List<RoundRankingItemResponse> ranking =
                roundRankingQueryService.rankingForRound(prelim.getId(), false);

        List<RoundRankingItemResponse> remaining = ranking.stream()
                .filter(r -> !ParticipationStatus.ADVANCED.name().equals(r.getParticipationStatus()))
                .filter(r -> !Objects.equals(r.getTeamId(), dqTeam.getId()))
                .filter(r -> isTeamEligibleForBackfill(r.getTeamId()))
                .filter(r -> topNVal <= 0 || r.getRank() == null || r.getRank() > topNVal)
                .collect(Collectors.toCollection(ArrayList::new));

        List<RoundRankingItemResponse> selected =
                WildcardCandidateSelection.selectExactSlots(remaining, 1);
        if (selected.isEmpty()) {
            auditService.log(AuditAction.DQ_WILDCARD_NO_CANDIDATE, "teams", dqTeam.getId(),
                    baseAudit(dqTeam, prelim, track, reason, null));
            return;
        }

        Integer candidateTeamId = selected.get(0).getTeamId();
        promoteCandidate(candidateTeamId, prelim, track, finalRound, dqTeam, reason,
                AuditAction.WILDCARD_BACKFILL, true);
    }

    private void promoteCandidate(Integer candidateTeamId, Round prelim, Track track,
                                  Round finalRound, Team dqTeam, String reason,
                                  String auditAction, boolean wildcardPath) {
        TeamRoundTrack candidateTrt = teamRoundTrackRepository
                .findByTeam_IdAndTrack_Id(candidateTeamId, track.getId())
                .or(() -> teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(candidateTeamId, prelim.getId()))
                .orElse(null);

        if (candidateTrt == null) {
            auditService.log(wildcardPath ? AuditAction.DQ_WILDCARD_NO_CANDIDATE
                            : AuditAction.DQ_NO_BACKFILL_BENCH_EMPTY,
                    "teams", dqTeam.getId(),
                    baseAudit(dqTeam, prelim, track, reason, candidateTeamId));
            return;
        }

        // Race-guard: candidate must still be eligible inside this transaction
        if (candidateTrt.getParticipationStatus() == ParticipationStatus.ADVANCED) {
            throw new ConflictException(ErrorCode.INVALID_STATE,
                    "Đội đôn #%d đã ADVANCED bởi thao tác khác — thử lại".formatted(candidateTeamId),
                    Map.of("teamId", candidateTeamId, "roundId", prelim.getId()));
        }
        if (!isTeamEligibleForBackfill(candidateTeamId)) {
            throw new ConflictException(ErrorCode.INVALID_STATE,
                    "Đội đôn #%d không còn eligible (đã DQ hoặc không tồn tại)".formatted(candidateTeamId),
                    Map.of("teamId", candidateTeamId));
        }

        candidateTrt.setParticipationStatus(ParticipationStatus.ADVANCED);
        teamRoundTrackRepository.save(candidateTrt);

        Team candidate = candidateTrt.getTeam();
        if (finalRound != null) {
            upsertFinalRoundParticipation(candidate, finalRound, dqTeam.getHackathon());
        }

        if (wildcardPath) {
            approveWildcardReviewIfPresent(prelim, candidate);
        }

        Map<String, Object> audit = baseAudit(dqTeam, prelim, track, reason, candidateTeamId);
        audit.put("backfillTeamName", candidate.getTeamName());
        audit.put("path", wildcardPath ? "WILDCARD" : "TOP_N");
        auditService.log(auditAction, "teams", candidateTeamId, audit);

        String trackLabel = track.getName() != null ? track.getName() : ("#" + track.getId());
        notifyTeam(candidate,
                "TEAM_BACKFILL_ADVANCE",
                "Đội được đôn vào Chung kết",
                "Đội \"" + candidate.getTeamName()
                        + "\" được đôn vào Chung kết thay ghế đội bị loại tại bảng "
                        + trackLabel + ".",
                candidate.getId());
    }

    private void approveWildcardReviewIfPresent(Round prelim, Team candidate) {
        Optional<WildcardReview> reviewOpt =
                wildcardReviewRepository.findByRound_IdAndTeam_Id(prelim.getId(), candidate.getId());
        if (reviewOpt.isEmpty()) {
            return;
        }
        WildcardReview review = reviewOpt.get();
        if (!Boolean.TRUE.equals(review.getCoordinatorApproved())) {
            review.setCoordinatorApproved(true);
            review.setCoordinatorNote("Auto-approved: DQ wildcard backfill");
            review.setReviewedAt(LocalDateTime.now());
            wildcardReviewRepository.save(review);
        }
    }

    private boolean isTeamEligibleForBackfill(Integer teamId) {
        if (teamId == null) {
            return false;
        }
        return teamRepository.findById(teamId)
                .map(t -> t.getStatus() != TeamStatus.ELIMINATED)
                .orElse(false);
    }

    private void removeFinalParticipation(Team team, Round finalRound) {
        if (finalRound == null) {
            return;
        }
        teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                .ifPresent(teamRoundParticipationRepository::delete);
    }

    private void upsertFinalRoundParticipation(Team team, Round finalRound, Hackathon hackathon) {
        teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                .orElseGet(() -> teamRoundParticipationRepository.save(TeamRoundParticipation.builder()
                        .team(team)
                        .round(finalRound)
                        .hackathon(hackathon)
                        .build()));
    }

    private void notifyTeam(Team team, String type, String title, String body, Integer referenceId) {
        List<User> members = teamMemberRepository.findByTeam_Id(team.getId()).stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .map(TeamMember::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        if (members.isEmpty()) {
            return;
        }
        notificationService.sendBatch(members, type, title, body, "teams", referenceId);
    }

    private static Map<String, Object> baseAudit(Team dqTeam, Round prelim, Track track,
                                                 String reason, Integer backfillTeamId) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("dqTeamId", dqTeam.getId());
        audit.put("dqTeamName", dqTeam.getTeamName());
        audit.put("reason", reason);
        audit.put("prelimRoundId", prelim != null ? prelim.getId() : null);
        audit.put("trackId", track != null ? track.getId() : null);
        audit.put("trackName", track != null ? track.getName() : null);
        if (backfillTeamId != null) {
            audit.put("backfillTeamId", backfillTeamId);
        }
        return audit;
    }

    private static boolean sameGroup(String a, String b) {
        if (!StringUtils.hasText(a) && !StringUtils.hasText(b)) {
            return true;
        }
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return Objects.equals(a, b);
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

}
