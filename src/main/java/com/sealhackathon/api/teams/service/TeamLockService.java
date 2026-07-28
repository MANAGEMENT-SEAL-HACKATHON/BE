package com.sealhackathon.api.teams.service;

/**
 * FR-13A — Khóa đội sau {@code registration_end} (cron idempotent).
 *
 * <p>Implemented by {@link com.sealhackathon.api.teams.service.impl.TeamLockServiceImpl}
 * and scheduled via {@link com.sealhackathon.api.teams.job.TeamLockScheduler}.
 */
public interface TeamLockService {

    /** Lock ACTIVE teams when registration period ended for ONGOING hackathons. */
    int lockTeamsAfterRegistrationEnd();
}
