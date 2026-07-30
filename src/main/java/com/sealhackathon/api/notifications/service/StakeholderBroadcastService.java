package com.sealhackathon.api.notifications.service;

import java.util.List;

/**
 * Fan-out in-app (+ optional email) notifications to all hackathon stakeholders.
 */
public interface StakeholderBroadcastService {

    void broadcast(Integer hackathonId, String type, String title, String body,
                   String referenceType, Integer referenceId, boolean sendEmail);

    /**
     * Builds body from {@code detailLines} (joined by newlines) then delegates to
     * {@link #broadcast(Integer, String, String, String, String, Integer, boolean)}.
     */
    void broadcast(Integer hackathonId, String type, String title, List<String> detailLines,
                   String referenceType, Integer referenceId, boolean sendEmail);
}
