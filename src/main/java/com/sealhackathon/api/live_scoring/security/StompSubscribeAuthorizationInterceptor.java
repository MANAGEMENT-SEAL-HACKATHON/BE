package com.sealhackathon.api.live_scoring.security;

import com.sealhackathon.api.auth.security.SealAuthentication;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
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

/** Chỉ COORDINATOR hoặc Judge được phân công mới SUBSCRIBE topic live scoring. */
@Component
@RequiredArgsConstructor
public class StompSubscribeAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern ROUND_TOPIC =
            Pattern.compile("^/topic/rounds/(\\d+)/(leaderboard-preview|scoring-progress)$");
    private static final Pattern TRACK_TOPIC =
            Pattern.compile("^/topic/tracks/(\\d+)/score-saved$");

    private final JudgeAssignmentGuard judgeAssignmentGuard;

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
        }
        return message;
    }
}
