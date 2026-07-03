package com.sealhackathon.api.config.seed;



import com.sealhackathon.api.chapters.entity.Chapter;

import com.sealhackathon.api.hackathons.entity.Hackathon;

import com.sealhackathon.api.hackathons.repository.HackathonRepository;

import com.sealhackathon.api.hackathons.value_object.HackathonStatus;

import com.sealhackathon.api.rounds.entity.Round;

import com.sealhackathon.api.rounds.repository.RoundRepository;

import com.sealhackathon.api.submissions.entity.Submission;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;

import com.sealhackathon.api.teams.entity.Team;

import com.sealhackathon.api.teams.repository.TeamRepository;

import com.sealhackathon.api.tracks.entity.Track;

import com.sealhackathon.api.users.entity.User;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Profile;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.List;



/**

 * GĐ4 — SL locked chưa publish, guest judge CK → activate CK fail RESULT_NOT_PUBLISHED (G4-N01/K2).

 */

@Slf4j

@Component

@Profile("dev")

@RequiredArgsConstructor

public class Gd4CkUnpublishedDataSeeder {



    private final HackathonDevSeedHelper seedHelper;

    private final HackathonRepository hackathonRepository;

    private final RoundRepository roundRepository;

    private final TeamRepository teamRepository;

    private final DevSeedCleanup devSeedCleanup;



    @Value("${app.seed.gd4.ck-unpublished.enabled:true}")

    private boolean enabled;



    @Transactional

    public void ensureSeed() {

        if (!enabled) {

            log.info("[Gd4CkUnpublishedDataSeeder] Tắt");

            return;

        }



        HackathonDevSeedHelper.PrelimState prelimState =

                new HackathonDevSeedHelper.PrelimState(false, true, true, false, 1, 4);

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(

                Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED,

                "SEAL GĐ4 — CK unpublished gate",

                HackathonStatus.ONGOING,

                "Prelim locked+scored unpublished, guest judge CK — activate CK blocked",

                prelimState,

                new HackathonDevSeedHelper.FinalState(false, false),

                seedHelper.computeGd4AdvanceReadyDates());



        Hackathon hackathon = structure.hackathon();

        Round prelim = structure.prelim();

        Round finalRound = structure.finalRound();

        Track track1 = structure.track1();

        Track track2 = structure.track2();



        seedHelper.syncHackathonCalendarFromDates(

                Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED,

                seedHelper.computeGd4AdvanceReadyDates());



        User coordinator = seedHelper.requireCoordinator();

        User judge1 = seedHelper.requireJudge1();

        User judge2 = seedHelper.requireJudge2();

        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime submittedAt = now.minusHours(72);



        List<Team> teams = new ArrayList<>();

        for (int i = 0; i < Gd4CkUnpublishedSeedConstants.TEAM_NAMES.length; i++) {

            int idx = i + 1;

            User leader = seedHelper.upsertStudent(

                    Gd4CkUnpublishedSeedConstants.studentEmail(idx),

                    Gd4CkUnpublishedSeedConstants.studentDisplayName(idx),

                    hcm);

            seedHelper.registerStudent(hackathon, leader);

            Team team = seedHelper.ensureActiveTeam(

                    hackathon, Gd4CkUnpublishedSeedConstants.TEAM_NAMES[i], leader, hcm, now);

            seedHelper.ensureTeamLocked(team, now);

            Track track = idx <= 2 ? track1 : track2;

            User judge = idx <= 2 ? judge1 : judge2;

            seedHelper.ensureLottery(

                    hackathon, prelim, track, Gd4CkUnpublishedSeedConstants.GROUPS[i], team, coordinator, now);

            Submission sub = seedHelper.ensurePrelimSubmission(

                    hackathon, prelim, track, team, SubmissionStatus.SUBMITTED, false, submittedAt);

            seedHelper.scoreAllTrackCriteria(sub, track, judge, Gd4CkUnpublishedSeedConstants.TEAM_SCORES[i], true);

            teams.add(team);

        }



        for (int advancedIndex : Gd4CkUnpublishedSeedConstants.ADVANCED_TEAM_INDICES) {

            seedHelper.markAdvanced(teams.get(advancedIndex), prelim, finalRound, hackathon);

        }



        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);

        prelim.setIsPublished(false);

        roundRepository.save(prelim);

        finalRound.setIsActive(false);

        roundRepository.save(finalRound);



        log.info("[Gd4CkUnpublishedDataSeeder] slug={} hackathonId={} prelim unpublished, CK guest assigned",

                Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED, hackathon.getId());

    }



    @Transactional

    public void repairForFeTesting() {

        if (!enabled) {

            return;

        }

        hackathonRepository.findBySlug(Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED).ifPresent(h -> {

            seedHelper.syncHackathonCalendarFromDates(

                    Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED,

                    seedHelper.computeGd4AdvanceReadyDates());

            Round prelim = loadPrelim(h.getId());

            Round finalRound = loadFinal(h.getId());

            if (prelim != null) {

                prelim.setIsPublished(false);

                roundRepository.save(prelim);

            }

            if (finalRound != null) {

                finalRound.setIsActive(false);

                roundRepository.save(finalRound);

                seedHelper.ensureFinalGuestJudgeAssignment(h, finalRound);

                reapplyAdvancedTeams(h, prelim, finalRound);

            }

        });

    }



    private void reapplyAdvancedTeams(Hackathon hackathon, Round prelim, Round finalRound) {

        for (int advancedIndex : Gd4CkUnpublishedSeedConstants.ADVANCED_TEAM_INDICES) {

            String teamName = Gd4CkUnpublishedSeedConstants.TEAM_NAMES[advancedIndex];

            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), teamName)

                    .ifPresent(team -> seedHelper.markAdvanced(team, prelim, finalRound, hackathon));

        }

    }



    private Round loadPrelim(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
    }

    private Round loadFinal(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()

                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))

                .findFirst()

                .orElse(null);

    }



    @Transactional

    public void resetAndSeed() {

        devSeedCleanup.purgeIfPresent(Gd4CkUnpublishedSeedConstants.SLUG_GD4_CK_UNPUBLISHED);

        ensureSeed();

    }

}

