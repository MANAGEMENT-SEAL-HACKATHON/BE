package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gate dùng chung: lottery + activate prelim — chặn khi còn đội PENDING.
 */
@Service
@RequiredArgsConstructor
public class PendingTeamGateService {

    private final TeamRepository teamRepository;

    public PendingTeamGateSnapshot snapshot(Integer hackathonId) {
        return snapshot(hackathonId, LocalDateTime.now());
    }

    public PendingTeamGateSnapshot snapshot(Integer hackathonId, LocalDateTime now) {
        List<Team> pending = teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.PENDING);
        int awaiting = 0;
        int grace = 0;
        int blocked = 0;
        LocalDateTime earliestGrace = null;

        for (Team team : pending) {
            if (team.getFormationSubmittedAt() != null) {
                awaiting++;
                continue;
            }
            LocalDateTime deadline = team.getFormationGraceDeadlineAt();
            if (deadline != null && deadline.isAfter(now)) {
                grace++;
                if (earliestGrace == null || deadline.isBefore(earliestGrace)) {
                    earliestGrace = deadline;
                }
            } else {
                blocked++;
            }
        }
        return new PendingTeamGateSnapshot(awaiting, grace, blocked, earliestGrace);
    }

    /** Ném TEAMS_PENDING_APPROVAL kèm metadata phân bucket nếu còn PENDING. */
    public void assertNoPendingTeams(Integer hackathonId) {
        PendingTeamGateSnapshot snap = snapshot(hackathonId);
        if (snap.hasPending()) {
            throw new BusinessRuleException(
                    ErrorCode.TEAMS_PENDING_APPROVAL,
                    snap.message(),
                    snap.details());
        }
    }
}
