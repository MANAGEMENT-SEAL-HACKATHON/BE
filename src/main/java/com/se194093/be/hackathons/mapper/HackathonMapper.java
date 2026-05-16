package com.se194093.be.hackathons.mapper;

import com.se194093.be.hackathons.dto.request.CreateHackathonRequest;
import com.se194093.be.hackathons.dto.request.UpdateHackathonRequest;
import com.se194093.be.hackathons.dto.response.HackathonResponse;
import com.se194093.be.hackathons.dto.response.HackathonSummaryResponse;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper plain Java: entity &lt;-&gt; DTO. Không dùng MapStruct ở phase này để không thêm dependency.
 */
@Component
public class HackathonMapper {

    public Hackathon toEntity(CreateHackathonRequest req) {
        return Hackathon.builder()
                .name(req.getName())
                .slug(req.getSlug())
                .season(req.getSeason())
                .year(req.getYear())
                .status(HackathonStatus.DRAFT)
                .description(req.getDescription())
                .rules(req.getRules())
                .bannerUrl(req.getBannerUrl())
                .registrationStart(req.getRegistrationStart())
                .registrationEnd(req.getRegistrationEnd())
                .eventStart(req.getEventStart())
                .eventEnd(req.getEventEnd())
                .wildcardEnabled(req.getWildcardEnabled() != null && req.getWildcardEnabled())
                .individualRankingEnabled(req.getIndividualRankingEnabled() != null && req.getIndividualRankingEnabled())
                .chapterScoringFormula(req.getChapterScoringFormula())
                .build();
    }

    public void applyUpdate(Hackathon entity, UpdateHackathonRequest req) {
        entity.setName(req.getName());
        entity.setSlug(req.getSlug());
        entity.setSeason(req.getSeason());
        entity.setYear(req.getYear());
        entity.setDescription(req.getDescription());
        entity.setRules(req.getRules());
        entity.setBannerUrl(req.getBannerUrl());
        entity.setRegistrationStart(req.getRegistrationStart());
        entity.setRegistrationEnd(req.getRegistrationEnd());
        entity.setEventStart(req.getEventStart());
        entity.setEventEnd(req.getEventEnd());
        if (req.getWildcardEnabled() != null) {
            entity.setWildcardEnabled(req.getWildcardEnabled());
        }
        if (req.getIndividualRankingEnabled() != null) {
            entity.setIndividualRankingEnabled(req.getIndividualRankingEnabled());
        }
        entity.setChapterScoringFormula(req.getChapterScoringFormula());
    }

    public HackathonResponse toResponse(Hackathon e) {
        if (e == null) {
            return null;
        }
        return HackathonResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .season(e.getSeason())
                .year(e.getYear())
                .status(e.getStatus())
                .description(e.getDescription())
                .rules(e.getRules())
                .bannerUrl(e.getBannerUrl())
                .registrationStart(e.getRegistrationStart())
                .registrationEnd(e.getRegistrationEnd())
                .eventStart(e.getEventStart())
                .eventEnd(e.getEventEnd())
                .wildcardEnabled(e.getWildcardEnabled())
                .individualRankingEnabled(e.getIndividualRankingEnabled())
                .chapterScoringFormula(e.getChapterScoringFormula())
                .createdById(e.getCreatedBy() == null ? null : e.getCreatedBy().getId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public HackathonSummaryResponse toSummary(Hackathon e) {
        if (e == null) {
            return null;
        }
        return HackathonSummaryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .season(e.getSeason())
                .year(e.getYear())
                .status(e.getStatus())
                .registrationStart(e.getRegistrationStart())
                .registrationEnd(e.getRegistrationEnd())
                .eventStart(e.getEventStart())
                .eventEnd(e.getEventEnd())
                .build();
    }
}
