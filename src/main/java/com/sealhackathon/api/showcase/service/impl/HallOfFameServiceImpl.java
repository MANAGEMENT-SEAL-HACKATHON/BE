package com.sealhackathon.api.showcase.service.impl;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.showcase.dto.response.HallOfFameEntryResponse;
import com.sealhackathon.api.showcase.entity.HallOfFameEntry;
import com.sealhackathon.api.showcase.mapper.ShowcaseMapper;
import com.sealhackathon.api.showcase.repository.HallOfFameEntryRepository;
import com.sealhackathon.api.showcase.service.HallOfFameService;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hall of Fame writes intentionally bypass {@code HackathonArchiveGuard}
 * (showcase content is created after FINISHED).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.showcase.enabled", havingValue = "true", matchIfMissing = true)
public class HallOfFameServiceImpl implements HallOfFameService {

    private final HallOfFameEntryRepository hallOfFameEntryRepository;
    private final HackathonRepository hackathonRepository;
    private final PrizeRepository prizeRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ShowcaseMapper showcaseMapper;

    @Override
    @Transactional
    public void snapshotFromFinishedHackathon(Integer hackathonId) {
        if (hallOfFameEntryRepository.existsByHackathonId(hackathonId)) {
            log.info("[showcase] HallOfFame already exists for hackathonId={}", hackathonId);
            return;
        }
        Hackathon hackathon = hackathonRepository.findById(hackathonId).orElse(null);
        if (hackathon == null || hackathon.getStatus() != HackathonStatus.FINISHED) {
            log.warn("[showcase] Skip HoF snapshot — hackathon missing or not FINISHED id={}", hackathonId);
            return;
        }
        Prize first = prizeRepository.findByHackathonIdAndPrizeRank(hackathonId, PrizeRank.FIRST).orElse(null);
        if (first == null || first.getTeam() == null) {
            log.warn("[showcase] Skip HoF snapshot — no FIRST prize for hackathonId={}", hackathonId);
            return;
        }
        hallOfFameEntryRepository.save(buildEntry(hackathon, first));
        log.info("[showcase] HallOfFame snapshot created hackathonId={} team={}", hackathonId, first.getTeam().getTeamName());
    }

    @Override
    @Transactional
    public int backfillFinishedHackathons() {
        int created = 0;
        for (Hackathon hackathon : hackathonRepository.findByStatus(HackathonStatus.FINISHED)) {
            if (hallOfFameEntryRepository.existsByHackathonId(hackathon.getId())) {
                continue;
            }
            Prize first = prizeRepository
                    .findByHackathonIdAndPrizeRank(hackathon.getId(), PrizeRank.FIRST)
                    .orElse(null);
            if (first == null || first.getTeam() == null) {
                continue;
            }
            hallOfFameEntryRepository.save(buildEntry(hackathon, first));
            created++;
        }
        log.info("[showcase] HallOfFame backfill created {} entries", created);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallOfFameEntryResponse> listPublic(Integer year) {
        List<HallOfFameEntry> entries = year == null
                ? hallOfFameEntryRepository.findAllByOrderByYearDescSeasonAsc()
                : hallOfFameEntryRepository.findByYearOrderBySeasonAsc(year);
        return entries.stream().map(showcaseMapper::toHofResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallOfFameEntryResponse> listByHackathon(Integer hackathonId) {
        return hallOfFameEntryRepository.findByHackathonId(hackathonId)
                .map(showcaseMapper::toHofResponse)
                .map(List::of)
                .orElse(List.of());
    }

    private HallOfFameEntry buildEntry(Hackathon hackathon, Prize first) {
        String members = teamMemberRepository.findByTeam_Id(first.getTeam().getId()).stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .map(this::memberLabel)
                .collect(Collectors.joining(", "));
        String trackName = first.getTrack() != null ? first.getTrack().getName() : null;
        return HallOfFameEntry.builder()
                .hackathonId(hackathon.getId())
                .hackathonName(hackathon.getName())
                .year(hackathon.getYear())
                .season(hackathon.getSeason())
                .teamId(first.getTeam().getId())
                .teamName(first.getTeam().getTeamName())
                .memberNames(members.isBlank() ? null : members)
                .trackName(trackName)
                .prizeName(first.getPrizeName())
                .prizeValue(first.getPrizeValue())
                .awardedAt(first.getAwardedAt())
                .build();
    }

    private String memberLabel(TeamMember m) {
        if (m.getUser() == null) {
            return "Unknown";
        }
        return m.getUser().getFullName();
    }
}
