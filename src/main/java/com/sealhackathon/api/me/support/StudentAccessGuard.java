package com.sealhackathon.api.me.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentAccessGuard {

    private final CurrentUserAccessor currentUserAccessor;
    private final TeamMemberRepository teamMemberRepository;

    public void assertTeamMember(Integer teamId) {
        Integer userId = currentUserAccessor.currentUserId();
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndStatus(
                userId, teamId, TeamMemberStatus.ACCEPTED)) {
            throw new AuthException(ErrorCode.FORBIDDEN,
                    "Bạn không thuộc đội này",
                    HttpStatus.FORBIDDEN);
        }
    }
}
