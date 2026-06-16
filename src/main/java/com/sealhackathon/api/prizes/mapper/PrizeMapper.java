package com.sealhackathon.api.prizes.mapper;

import com.sealhackathon.api.prizes.dto.response.PrizeResponse;
import com.sealhackathon.api.prizes.entity.Prize;
import org.springframework.stereotype.Component;

@Component
public class PrizeMapper {

    public PrizeResponse toResponse(Prize p) {
        if (p == null) {
            return null;
        }
        Integer hackathonId = p.getRound() != null && p.getRound().getHackathon() != null
                ? p.getRound().getHackathon().getId()
                : null;
        return PrizeResponse.builder()
                .id(p.getId())
                .hackathonId(hackathonId)
                .roundId(p.getRound() != null ? p.getRound().getId() : null)
                .trackId(p.getTrack() != null ? p.getTrack().getId() : null)
                .teamId(p.getTeam() != null ? p.getTeam().getId() : null)
                .teamName(p.getTeam() != null ? p.getTeam().getTeamName() : null)
                .prizeName(p.getPrizeName())
                .prizeRank(p.getPrizeRank())
                .prizeValue(p.getPrizeValue())
                .description(p.getDescription())
                .awardedAt(p.getAwardedAt())
                .awardedById(p.getAwardedBy() != null ? p.getAwardedBy().getId() : null)
                .build();
    }
}
