package com.sealhackathon.api.teams.dto.response;

import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponse {

    private Integer id;
    private Integer hackathonId;
    private String teamName;
    private Integer leaderId;
    private Integer chapterId;
    private TeamStatus status;
    private Boolean isLocked;
    private LocalDateTime createdAt;
}
