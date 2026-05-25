package com.sealhackathon.api.teams.mapper;

import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.teams.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team entity) {
        if (entity == null) {
            return null;
        }
        return TeamResponse.builder()
                .id(entity.getId())
                .hackathonId(entity.getHackathon() != null ? entity.getHackathon().getId() : null)
                .teamName(entity.getTeamName())
                .leaderId(entity.getLeader() != null ? entity.getLeader().getId() : null)
                .chapterId(entity.getChapter() != null ? entity.getChapter().getId() : null)
                .status(entity.getStatus())
                .isLocked(entity.getIsLocked())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
