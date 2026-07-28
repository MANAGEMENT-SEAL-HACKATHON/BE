package com.sealhackathon.api.presentation.support;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JudgePresenceRegistry {

    private final ConcurrentHashMap<Integer, Instant> lastSeen = new ConcurrentHashMap<>();

    public void heartbeat(Integer judgeId) {
        if (judgeId != null) {
            lastSeen.put(judgeId, Instant.now());
        }
    }

    public Instant lastSeenAt(Integer judgeId) {
        return judgeId == null ? null : lastSeen.get(judgeId);
    }

    public boolean isOnline(Integer judgeId, long maxAgeSeconds) {
        Instant at = lastSeenAt(judgeId);
        if (at == null) {
            return false;
        }
        return Instant.now().minusSeconds(maxAgeSeconds).isBefore(at);
    }

    public Map<Integer, Instant> snapshot() {
        return Map.copyOf(lastSeen);
    }
}
