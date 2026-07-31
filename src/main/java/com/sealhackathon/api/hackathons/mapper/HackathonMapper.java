package com.sealhackathon.api.hackathons.mapper;

import com.sealhackathon.api.hackathons.dto.request.CreateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.request.UpdateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonSummaryResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.support.HackathonBannerUrls;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import org.springframework.stereotype.Component;

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
                .registrationStart(req.getRegistrationStart())
                .registrationEnd(req.getRegistrationEnd())
                .eventStart(req.getEventStart())
                .eventEnd(req.getEventEnd())
                .individualRankingEnabled(req.getIndividualRankingEnabled() != null && req.getIndividualRankingEnabled())
                .chapterScoringFormula(req.getChapterScoringFormula())
                .maxParticipants(req.getMaxParticipants())
                .build();
    }

    public void applyUpdate(Hackathon entity, UpdateHackathonRequest req) {
        entity.setName(req.getName());
        entity.setSlug(req.getSlug());
        entity.setSeason(req.getSeason());
        entity.setYear(req.getYear());
        entity.setDescription(req.getDescription());
        entity.setRules(req.getRules());
        entity.setRegistrationStart(req.getRegistrationStart());
        entity.setRegistrationEnd(req.getRegistrationEnd());
        entity.setEventStart(req.getEventStart());
        entity.setEventEnd(req.getEventEnd());
        if (req.getIndividualRankingEnabled() != null) {
            entity.setIndividualRankingEnabled(req.getIndividualRankingEnabled());
        }
        entity.setChapterScoringFormula(req.getChapterScoringFormula());
        if (req.getMaxParticipants() != null) {
            entity.setMaxParticipants(req.getMaxParticipants());
        }
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
                .registrationPhase(HackathonRegistrationSupport.resolveRegistrationPhase(e))
                .description(e.getDescription())
                .rules(e.getRules())
                .bannerUrl(HackathonBannerUrls.resolveForResponse(e))
                .registrationStart(e.getRegistrationStart())
                .registrationEnd(e.getRegistrationEnd())
                .registrationClosedEarlyAt(e.getRegistrationClosedEarlyAt())
                .registrationExtensionCount(e.getRegistrationExtensionCount())
                .registrationExtendedAt(e.getRegistrationExtendedAt())
                .scheduleAdjustedAt(e.getScheduleAdjustedAt())
                .eventStart(e.getEventStart())
                .eventEnd(e.getEventEnd())
                .individualRankingEnabled(e.getIndividualRankingEnabled())
                .chapterScoringFormula(e.getChapterScoringFormula())
                .createdById(e.getCreatedBy() == null ? null : e.getCreatedBy().getId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .maxParticipants(e.getMaxParticipants())
                .clonedFromHackathonId(e.getClonedFromHackathon() == null ? null : e.getClonedFromHackathon().getId())
                .clonedFromHackathonName(e.getClonedFromHackathon() == null ? null : e.getClonedFromHackathon().getName())
                .clonedAt(e.getClonedAt())
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
                .registrationPhase(HackathonRegistrationSupport.resolveRegistrationPhase(e))
                .registrationStart(e.getRegistrationStart())
                .registrationEnd(e.getRegistrationEnd())
                .eventStart(e.getEventStart())
                .eventEnd(e.getEventEnd())
                .maxParticipants(e.getMaxParticipants())
                .bannerUrl(HackathonBannerUrls.resolveForResponse(e))
                .clonedFromHackathonId(e.getClonedFromHackathon() == null ? null : e.getClonedFromHackathon().getId())
                .clonedFromHackathonName(e.getClonedFromHackathon() == null ? null : e.getClonedFromHackathon().getName())
                .clonedAt(e.getClonedAt())
                .build();
    }
}