package com.sealhackathon.api.live_scoring.security;

import com.sealhackathon.api.auth.security.SealAuthentication;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chỉ COORDINATOR, Judge được phân công, hoặc presentation controller mới SUBSCRIBE topic live. */
@Component
@RequiredArgsConstructor
public class StompSubscribeAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern ROUND_TOPIC =
            Pattern.compile("^/topic/rounds/(\\d+)/(leaderboard-preview|scoring-progress)$");
    private static final Pattern TRACK_TOPIC =
            Pattern.compile("^/topic/tracks/(\\d+)/score-saved$");
    private static final Pattern PRESENTATION_TRACK_TOPIC =
            Pattern.compile("^/topic/rounds/(\\d+)/tracks/(\\d+)/presentation-queue$");
    private static final Pattern PRESENTATION_ROUND_TOPIC =
            Pattern.compile("^/topic/rounds/(\\d+)/presentation-queue$");

    private final JudgeAssignmentGuard judgeAssignmentGuard;
    private final PresentationControllerGuard presentationControllerGuard;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        Principal user = accessor.getUser();
        if (!(user instanceof SealAuthentication auth)) {
            throw new AccessDeniedException("Chưa xác thực WebSocket");
        }
        CurrentUserStub stub = auth.getPrincipal();
        boolean coordinator = stub.getRole() == UserRole.COORDINATOR;
        String dest = accessor.getDestination();
        if (dest == null) {
            return message;
        }

        Matcher roundMatcher = ROUND_TOPIC.matcher(dest);
        if (roundMatcher.matches()) {
            Integer roundId = Integer.parseInt(roundMatcher.group(1));
            if (!judgeAssignmentGuard.canAccessRound(stub.getUserId(), roundId, coordinator)) {
                throw new AccessDeniedException("Không có quyền subscribe round " + roundId);
            }
            return message;
        }

        Matcher trackMatcher = TRACK_TOPIC.matcher(dest);
        if (trackMatcher.matches()) {
            Integer trackId = Integer.parseInt(trackMatcher.group(1));
            if (!judgeAssignmentGuard.canAccessTrack(stub.getUserId(), trackId, coordinator)) {
                throw new AccessDeniedException("Không có quyền subscribe track " + trackId);
            }
            return message;
        }

        Matcher presentationTrackMatcher = PRESENTATION_TRACK_TOPIC.matcher(dest);
        if (presentationTrackMatcher.matches()) {
            Integer roundId = Integer.parseInt(presentationTrackMatcher.group(1));
            Integer trackId = Integer.parseInt(presentationTrackMatcher.group(2));
            if (!canAccessPresentationQueue(stub, coordinator, roundId, trackId)) {
                throw new AccessDeniedException("Không có quyền subscribe presentation queue track " + trackId);
            }
            return message;
        }

        Matcher presentationRoundMatcher = PRESENTATION_ROUND_TOPIC.matcher(dest);
        if (presentationRoundMatcher.matches()) {
            Integer roundId = Integer.parseInt(presentationRoundMatcher.group(1));
            if (!canAccessPresentationRoundQueue(stub, coordinator, roundId)) {
                throw new AccessDeniedException("Không có quyền subscribe presentation queue round " + roundId);
            }
        }
        return message;
    }

    private boolean canAccessPresentationQueue(CurrentUserStub stub, boolean coordinator,
                                               Integer roundId, Integer trackId) {
        if (coordinator) {
            return true;
        }
        if (judgeAssignmentGuard.canAccessTrack(stub.getUserId(), trackId, false)) {
            return true;
        }
        Track track = trackRepository.findById(trackId).orElse(null);
        if (track == null) {
            return false;
        }
        Round round = roundRepository.findById(roundId).orElse(track.getRound());
        return presentationControllerGuard.canControlTrack(stub.getUserId(), track, round, false);
    }

    private boolean canAccessPresentationRoundQueue(CurrentUserStub stub, boolean coordinator, Integer roundId) {
        if (coordinator) {
            return true;
        }
        if (judgeAssignmentGuard.canAccessRound(stub.getUserId(), roundId, false)) {
            return true;
        }
        Round round = roundRepository.findById(roundId).orElse(null);
        if (round == null) {
            return false;
        }
        return presentationControllerGuard.canControlRound(stub.getUserId(), round, false);
    }
}
