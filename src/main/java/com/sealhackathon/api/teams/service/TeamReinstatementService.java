package com.sealhackathon.api.teams.service;

import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.teams.entity.Team;

/**
 * Reinstate a manually DQ'd team after an approved appeal (pre-advance only).
 */
public interface TeamReinstatementService {

    void reinstateFromAppeal(Team team, Appeal appeal);
}
