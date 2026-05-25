package com.sealhackathon.api.invitations.service;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.users.entity.User;

/**
 * FR-05a — Vòng đời judge khách: cửa sổ resend trước KICKOFF, khóa sau {@code event_end}.
 */
public interface GuestJudgeLifecycleService {

    int RESEND_CUTOFF_HOURS_BEFORE_KICKOFF = 48;

    void assertHackathonNotEndedForTempJudge(User user);

    void assertHackathonNotEnded(Hackathon hackathon);

    void assertResendAllowed(Invitation invitation);

    void requireHackathonOnInvitation(Invitation invitation);

    Hackathon requireHackathonOnInvitationEntity(Invitation invitation);
}
