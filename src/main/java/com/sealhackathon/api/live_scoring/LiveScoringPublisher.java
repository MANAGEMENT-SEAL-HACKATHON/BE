package com.sealhackathon.api.live_scoring;

import com.sealhackathon.api.live_scoring.event.LiveScoreSavedEvent;
import com.sealhackathon.api.live_scoring.event.ScoringLockedEvent;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.dto.response.ScoreResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/** FR-18A — broadcast leaderboard/progress sau mỗi score (debounce 300ms). */
@Component
@RequiredArgsConstructor
@Slf4j
public class LiveScoringPublisher {

    private static final long DEBOUNCE_MS = 300;

    private final SimpMessagingTemplate messagingTemplate;
    private final RoundRankingQueryService roundRankingQueryService;
    private final ScoringProgressQueryService scoringProgressQueryService;
    private final RoundRepository roundRepository;
    private final TaskScheduler taskScheduler;

    private final Map<Integer, ScheduledFuture<?>> pendingRoundBroadcast = new ConcurrentHashMap<>();

    @EventListener
    public void onScoreSaved(LiveScoreSavedEvent event) {
        Integer roundId = event.getRoundId();
        Integer trackId = event.getTrackId();
        ScoreResponse score = event.getScore();

        if (trackId != null) {
            messagingTemplate.convertAndSend("/topic/tracks/" + trackId + "/score-saved", score);
        }

        scheduleRoundBroadcast(roundId);
    }

    @EventListener
    public void onScoringLocked(ScoringLockedEvent event) {
        pendingRoundBroadcast.remove(event.getRoundId());
        Round round = roundRepository.findById(event.getRoundId()).orElse(null);
        if (round == null) {
            return;
        }
        RoundScoringProgressResponse progress = scoringProgressQueryService.progressForRound(round);
        messagingTemplate.convertAndSend(
                "/topic/rounds/" + event.getRoundId() + "/scoring-progress", progress);
    }

    private void scheduleRoundBroadcast(Integer roundId) {
        ScheduledFuture<?> existing = pendingRoundBroadcast.remove(roundId);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> scheduled = taskScheduler.schedule(
                () -> broadcastRound(roundId),
                Instant.now().plusMillis(DEBOUNCE_MS));
        pendingRoundBroadcast.put(roundId, scheduled);
    }

    private void broadcastRound(Integer roundId) {
        pendingRoundBroadcast.remove(roundId);
        try {
            Round round = roundRepository.findById(roundId).orElse(null);
            if (round == null || Boolean.TRUE.equals(round.getScoringLocked())) {
                return;
            }
            List<RoundRankingItemResponse> ranking =
                    roundRankingQueryService.rankingForRound(roundId, true);
            RoundScoringProgressResponse progress =
                    scoringProgressQueryService.progressForRound(round);
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/leaderboard-preview", ranking);
            messagingTemplate.convertAndSend(
                    "/topic/rounds/" + roundId + "/scoring-progress", progress);
        } catch (Exception ex) {
            log.warn("[LiveScoring] broadcast round #{} failed: {}", roundId, ex.getMessage());
        }
    }
}
