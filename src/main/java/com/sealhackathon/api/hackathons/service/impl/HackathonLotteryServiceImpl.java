package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.dto.request.HackathonLotteryRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonLotteryResponse;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.HackathonLotteryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MF-02 FR-13B — Bốc thăm Track (batch).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HackathonLotteryServiceImpl implements HackathonLotteryService {

    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;

    @Override
    public HackathonLotteryResponse runLottery(Integer hackathonId, HackathonLotteryRequest req) {
        // TODO FR-13B: validate round PRELIMINARY; teams ACTIVE + locked; tracks OPEN;
        // TODO: per assignment INSERT team_round_participation THEN team_round_tracks (same TX);
        // TODO: TRACK_GROUP_FULL, TEAM_ALREADY_IN_TRACK_THIS_ROUND, audit TRACK_TOPIC_UPDATE
        throw new UnsupportedOperationException("TODO: implement runLottery");
    }
}
