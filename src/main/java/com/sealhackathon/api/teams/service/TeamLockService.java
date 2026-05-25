package com.sealhackathon.api.teams.service;

/**
 * FR-13A — Khóa đội sau {@code registration_end} (cron idempotent).
 */
public interface TeamLockService {

    /** TODO: quét hackathon ONGOING, set teams.is_locked=TRUE cho ACTIVE. */
    int lockTeamsAfterRegistrationEnd();
}
