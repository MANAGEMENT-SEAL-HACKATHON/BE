package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * GĐ3 — prelim inactive, judge/track OK, 0 đội trong round → NO_TEAMS_IN_ROUND.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3NoLotteryDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.no-lottery.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3NoLotteryDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3NoLotterySeedConstants.SLUG_GD3_NO_LOTTERY,
                "SEAL GĐ3 — No lottery",
                HackathonStatus.ONGOING,
                "Prelim inactive, 0 participation — activate → NO_TEAMS_IN_ROUND (G3-N01)",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        seedHelper.syncHackathonCalendarFromDates(
                Gd3NoLotterySeedConstants.SLUG_GD3_NO_LOTTERY, seedHelper.computeGd3ActivePrelimDates());

        teamRoundParticipationRepository.findByRound_Id(prelim.getId())
                .forEach(teamRoundParticipationRepository::delete);

        log.info("[Gd3NoLotteryDataSeeder] slug={} hackathonId={} prelimRoundId={}",
                Gd3NoLotterySeedConstants.SLUG_GD3_NO_LOTTERY, hackathon.getId(), prelim.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3NoLotterySeedConstants.SLUG_GD3_NO_LOTTERY).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            if (prelim != null) {
                teamRoundParticipationRepository.findByRound_Id(prelim.getId())
                        .forEach(teamRoundParticipationRepository::delete);
            }
        });
    }

    private Round loadPrelim(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3NoLotterySeedConstants.SLUG_GD3_NO_LOTTERY);
        ensureSeed();
    }
}
