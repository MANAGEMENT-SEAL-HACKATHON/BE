package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.dto.response.TeamJourneyResponse;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamJourneyService;
import com.sealhackathon.api.teams.support.TeamAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamJourneyServiceImpl implements TeamJourneyService {

    private final TeamRepository teamRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TeamAccessGuard teamAccessGuard;

    @Override
    public TeamJourneyResponse getJourney(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        // Chống IDOR: chỉ coordinator / mentor phụ trách / thành viên đội mới xem được hành trình.
        teamAccessGuard.assertCanViewTeamDetails(teamId);

        Map<Integer, TeamJourneyResponse.JourneyStep> byRound = new LinkedHashMap<>();

        for (TeamRoundTrack trt : teamRoundTrackRepository.findByTeamIdWithTrackAndRound(teamId)) {
            Round round = trt.getTrack().getRound();
            byRound.put(round.getId(), TeamJourneyResponse.JourneyStep.builder()
                    .roundId(round.getId())
                    .roundName(round.getName())
                    .trackId(trt.getTrack().getId())
                    .trackName(trt.getTrack().getName())
                    .participationStatus(trt.getParticipationStatus().name())
                    .build());
        }

        for (TeamRoundParticipation trp : teamRoundParticipationRepository.findByTeamIdWithRound(teamId)) {
            Round round = trp.getRound();
            byRound.putIfAbsent(round.getId(), TeamJourneyResponse.JourneyStep.builder()
                    .roundId(round.getId())
                    .roundName(round.getName())
                    .participationStatus("PARTICIPATING")
                    .build());
        }

        List<TeamJourneyResponse.JourneyStep> steps = new ArrayList<>(byRound.values());
        steps.sort(Comparator.comparing(TeamJourneyResponse.JourneyStep::getRoundId));

        return TeamJourneyResponse.builder()
                .teamId(teamId)
                .teamName(team.getTeamName())
                .steps(steps)
                .build();
    }
}
