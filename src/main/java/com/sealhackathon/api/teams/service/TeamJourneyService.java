package com.sealhackathon.api.teams.service;

import com.sealhackathon.api.teams.dto.response.TeamJourneyResponse;

public interface TeamJourneyService {

    TeamJourneyResponse getJourney(Integer teamId);
}
