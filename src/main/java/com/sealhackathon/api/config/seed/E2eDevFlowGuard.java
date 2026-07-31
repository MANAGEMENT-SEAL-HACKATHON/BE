package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Freeze guard cho {@code seal-e2e-2026}: khi coordinator đã đi qua GĐ2 (lottery / activate / nộp…),
 * mọi repair startup phá workflow phải bỏ qua — trừ khi {@code app.seed.e2e.force-gd2-reset=true}.
 *
 * <p>Cam kết demo hội đồng A–Z với {@code ddl-auto=update}: restart BE không mất dữ liệu GĐ3–GĐ6.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class E2eDevFlowGuard {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final SubmissionRepository submissionRepository;

    @Value("${app.seed.e2e.force-gd2-reset:false}")
    private boolean forceGd2Reset;

    public boolean isForceGd2Reset() {
        return forceGd2Reset;
    }

    /**
     * {@code true} = đang demo GĐ3+ — cấm timeline / GĐ2 / GĐ5 repair trên slug E2E.
     * Bypass khi {@code force-gd2-reset=true}.
     */
    public boolean isE2eFlowFrozen() {
        if (forceGd2Reset) {
            return false;
        }
        return hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING)
                .map(this::isFrozen)
                .orElse(false);
    }

    public boolean isFrozen(Hackathon hackathon) {
        if (hackathon == null || hackathon.getId() == null) {
            return false;
        }
        if (forceGd2Reset) {
            return false;
        }
        if (hackathon.getStatus() == HackathonStatus.PENDING_CONFIRM
                || hackathon.getStatus() == HackathonStatus.FINISHED) {
            return true;
        }

        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
        Round prelim = rounds.stream()
                .filter(r -> r.getRoundType() == RoundType.PRELIMINARY
                        || !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        Round finalRound = rounds.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);

        if (prelim != null && hasPrelimProgress(prelim)) {
            return true;
        }
        if (finalRound != null && hasFinalProgress(finalRound)) {
            return true;
        }

        if (hasLotteryAssignments(rounds)) {
            return true;
        }
        if (!submissionRepository.findByHackathon_Id(hackathon.getId()).isEmpty()) {
            return true;
        }
        if (allActiveTeamsLocked(hackathon.getId())) {
            return true;
        }
        return false;
    }

    /** Pure helper — dùng unit test không cần Spring context. */
    public static boolean hasPrelimProgress(Round prelim) {
        if (prelim == null) {
            return false;
        }
        return prelim.getActivatedAt() != null
                || Boolean.TRUE.equals(prelim.getIsActive())
                || prelim.getProblemReleasedAt() != null
                || prelim.getSubmissionClosedEarlyAt() != null
                || Boolean.TRUE.equals(prelim.getScoringLocked())
                || Boolean.TRUE.equals(prelim.getIsPublished());
    }

    public static boolean hasFinalProgress(Round finalRound) {
        if (finalRound == null) {
            return false;
        }
        return finalRound.getActivatedAt() != null
                || Boolean.TRUE.equals(finalRound.getIsActive())
                || Boolean.TRUE.equals(finalRound.getScoringLocked());
    }

    private boolean hasLotteryAssignments(List<Round> rounds) {
        for (Round r : rounds) {
            if (r.getId() == null) {
                continue;
            }
            if (!teamRoundTrackRepository.findByTrack_Round_Id(r.getId()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean allActiveTeamsLocked(Integer hackathonId) {
        List<Team> active = teamRepository.findByHackathon_Id(hackathonId).stream()
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                .toList();
        if (active.isEmpty()) {
            return false;
        }
        return active.stream().allMatch(t -> Boolean.TRUE.equals(t.getIsLocked()));
    }

    public void logSkip(String repairName) {
        log.info(
                "[E2eDevFlowGuard] Bỏ qua {} — flow đang GĐ3+ (force-gd2-reset=false)",
                repairName);
    }
}
