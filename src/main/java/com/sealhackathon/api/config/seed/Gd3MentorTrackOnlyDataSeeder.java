package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * GĐ3 — mentor có track assignment, chưa có mentor-team → FR-M-05 bootstrap.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3MentorTrackOnlyDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.mentor-track-only.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3MentorTrackOnlyDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3MentorTrackOnlySeedConstants.SLUG_GD3_MENTOR_TRACK_ONLY,
                "SEAL GĐ3 — Mentor track only",
                HackathonStatus.ONGOING,
                "Mentor gán track, chưa gán đội — FR-M-05 bootstrap",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(
                Gd3MentorTrackOnlySeedConstants.SLUG_GD3_MENTOR_TRACK_ONLY,
                seedHelper.computeGd3ActivePrelimDates());

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User mentor = seedHelper.upsertMentor(
                Gd3MentorTrackOnlySeedConstants.EMAIL_MENTOR_TRACK_ONLY,
                "Mentor Track Only",
                hcm);
        seedHelper.clearMentorTeamAssignmentsForMentor(mentor.getId());
        seedHelper.clearMentorTeamAssignmentsForHackathon(hackathon.getId());
        seedHelper.clearMentorAssignments(track1);
        seedHelper.clearMentorAssignments(structure.track2());
        seedHelper.ensureMentorTrackAssignment(track1, mentor, coordinator);

        log.info("[Gd3MentorTrackOnlyDataSeeder] slug={} hackathonId={} mentor={}",
                Gd3MentorTrackOnlySeedConstants.SLUG_GD3_MENTOR_TRACK_ONLY,
                hackathon.getId(),
                Gd3MentorTrackOnlySeedConstants.EMAIL_MENTOR_TRACK_ONLY);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        ensureSeed();
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3MentorTrackOnlySeedConstants.SLUG_GD3_MENTOR_TRACK_ONLY);
        ensureSeed();
    }
}
