package com.sealhackathon.api.teams.dto.response;

import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberResponse {

    private Integer userId;
    private String fullName;
    private String email;
    private TeamMemberRole roleInTeam;
    private TeamMemberStatus status;
    private UserStatus userAccountStatus;
}
