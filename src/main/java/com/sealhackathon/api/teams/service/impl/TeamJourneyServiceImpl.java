package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.teams.dto.response.TeamJourneyResponse;
import com.sealhackathon.api.teams.service.TeamJourneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamJourneyServiceImpl implements TeamJourneyService {

    @Override
    public TeamJourneyResponse getJourney(Integer teamId) {
        // TODO: Build journey from team_round_tracks + round progression artifacts.
        return TeamJourneyResponse.builder()
                .teamId(teamId)
                .steps(List.of())
                .build();
    }
}
