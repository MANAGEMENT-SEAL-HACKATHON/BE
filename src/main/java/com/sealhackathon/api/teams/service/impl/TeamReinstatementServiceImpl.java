package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.service.TeamReinstatementService;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamReinstatementServiceImpl implements TeamReinstatementService {

    private final TeamRepository teamRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final AuditService auditService;

    @Override
    public void reinstateFromAppeal(Team team, Appeal appeal) {
        Integer roundId = appeal.getRound().getId();

        boolean anyAdvanced = teamRoundTrackRepository.findByTrack_Round_Id(roundId).stream()
                .anyMatch(trt -> trt.getParticipationStatus() == ParticipationStatus.ADVANCED);
        if (anyAdvanced) {
            throw new BusinessRuleException(ErrorCode.APPEAL_APPROVE_AFTER_ADVANCE,
                    "Không thể phục hồi đội sau khi đã chốt chuyển vòng",
                    Map.of("roundId", roundId, "teamId", team.getId()));
        }

        team.setStatus(TeamStatus.ACTIVE);
        team.setEliminatedAt(null);
        team.setEliminationReason(null);
        teamRepository.save(team);

        teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), roundId)
                .ifPresent(trt -> {
                    trt.setParticipationStatus(ParticipationStatus.PARTICIPATING);
                    teamRoundTrackRepository.save(trt);
                });

        auditService.log(AuditAction.TEAM_REINSTATE_APPEAL, "teams", team.getId(),
                Map.of("appealId", appeal.getId(),
                        "roundId", roundId,
                        "teamName", team.getTeamName()));
    }
}
