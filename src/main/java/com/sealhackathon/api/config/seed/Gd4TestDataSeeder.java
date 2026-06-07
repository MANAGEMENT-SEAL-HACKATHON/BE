package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seed GĐ4 tùy chọn — tiebreak 3-way + wildcard trên {@link GdExtendedSeedConstants#SLUG_GD4_TIEBREAK}.
 *
 * <p>Bật: {@code app.seed.gd4.enabled=true} (mặc định {@code false}).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4TestDataSeeder {

    private final DevSeedProperties devSeedProperties;
    private final HackathonDevSeedHelper seedHelper;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final CriteriaRepository criteriaRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seedTiebreakAndWildcardData() {
        if (!devSeedProperties.isGd4Enabled()) {
            log.debug("[Gd4TestDataSeeder] Bỏ qua — app.seed.gd4.enabled=false");
            return;
        }

        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD4_TIEBREAK,
                "SEAL GĐ4 Tiebreak & Wildcard",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — đồng điểm tiebreak + wildcard (opt-in).",
                new HackathonDevSeedHelper.PrelimState(false, true, false, false, 1, 3),
                new HackathonDevSeedHelper.FinalState(false, false));

        ensureGd4Teams(structure);

        Round prelim = structure.prelim();
        Track track1 = structure.track1();
        int hackathonId = structure.hackathon().getId();

        Team team01 = teamByName(hackathonId, GdExtendedSeedConstants.GD4_TEAM_01);
        Team team02 = teamByName(hackathonId, GdExtendedSeedConstants.GD4_TEAM_02);
        Team team03 = teamByName(hackathonId, GdExtendedSeedConstants.GD4_TEAM_03);
        Team team04 = teamByName(hackathonId, GdExtendedSeedConstants.GD4_TEAM_04);
        Team team05 = teamByName(hackathonId, GdExtendedSeedConstants.GD4_TEAM_05);

        moveToGroup(team02, prelim, track1, "Bảng A");
        moveToGroup(team03, prelim, track1, "Bảng A");

        moveToGroup(team04, prelim, track1, "Bảng B");
        moveToGroup(team05, prelim, track1, "Bảng B");

        User judge1 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE1).orElseThrow();

        upsertSubmissionAndScore(team01, prelim, track1, judge1, 10.0);
        upsertSubmissionAndScore(team02, prelim, track1, judge1, 10.0);
        upsertSubmissionAndScore(team03, prelim, track1, judge1, 10.0);

        upsertSubmissionAndScore(team05, prelim, track1, judge1, 9.5);
        upsertSubmissionAndScore(team04, prelim, track1, judge1, 9.0);

        prelim.setTopNAdvance(1);
        prelim.setMinTeamsFinal(3);
        prelim.setWildcardEnabled(true);
        prelim.setScoringLocked(true);
        prelim.setIsPublished(true);
        prelim.setScoringLockedAt(LocalDateTime.now());
        prelim.setPublishedAt(LocalDateTime.now());
        roundRepository.save(prelim);

        log.info("[Gd4TestDataSeeder] Tiebreak/wildcard seed trên slug={} (app.seed.gd4.enabled=true)",
                GdExtendedSeedConstants.SLUG_GD4_TIEBREAK);
    }

    private void ensureGd4Teams(HackathonDevSeedHelper.HackathonStructure structure) {
        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(
                structure.hackathon().getId(), GdExtendedSeedConstants.GD4_TEAM_01)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        var hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        String[] names = {
                GdExtendedSeedConstants.GD4_TEAM_01,
                GdExtendedSeedConstants.GD4_TEAM_02,
                GdExtendedSeedConstants.GD4_TEAM_03,
                GdExtendedSeedConstants.GD4_TEAM_04,
                GdExtendedSeedConstants.GD4_TEAM_05
        };
        String[] emails = {
                GdExtendedSeedConstants.GD4_STU_01,
                GdExtendedSeedConstants.GD4_STU_02,
                GdExtendedSeedConstants.GD4_STU_03,
                GdExtendedSeedConstants.GD4_STU_04,
                GdExtendedSeedConstants.GD4_STU_05
        };
        for (int i = 0; i < names.length; i++) {
            User leader = seedHelper.upsertStudent(emails[i], "GD4 Leader " + (i + 1), hcm);
            Team team = seedHelper.ensureActiveTeam(structure.hackathon(), names[i], leader, hcm, now);
            seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(),
                    i < 3 ? "Bảng A" : "Bảng B", team, coordinator, now);
        }
    }

    private Team teamByName(Integer hackathonId, String teamName) {
        return teamRepository.findByHackathon_Id(hackathonId).stream()
                .filter(t -> teamName.equals(t.getTeamName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu team seed: " + teamName));
    }

    private void moveToGroup(Team team, Round round, Track track, String group) {
        TeamRoundTrack trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), round.getId())
                .orElseThrow();
        trt.setTrack(track);
        trt.setAssignedGroup(group);
        teamRoundTrackRepository.save(trt);
    }

    private void upsertSubmissionAndScore(Team team, Round round, Track track, User judge, double targetScore) {
        Submission sub = submissionRepository.findByRound_Id(round.getId()).stream()
                .filter(s -> s.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElse(null);

        if (sub == null) {
            sub = submissionRepository.save(Submission.builder()
                    .team(team)
                    .round(round)
                    .hackathon(round.getHackathon())
                    .track(track)
                    .status(SubmissionStatus.SUBMITTED)
                    .submittedAt(LocalDateTime.now())
                    .repoUrl("https://github.com/test/gd4-" + team.getId())
                    .build());
        }

        Criteria crit = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).stream()
                .findFirst()
                .orElseThrow();

        if (scoreRepository.findBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                sub.getId(), crit.getId(), ScoreType.NORMAL, true).isEmpty()) {
            scoreRepository.save(Score.builder()
                    .submission(sub)
                    .criterion(crit)
                    .judge(judge)
                    .scoreValue((float) targetScore)
                    .scoreType(ScoreType.NORMAL)
                    .isFinal(true)
                    .scoredAt(LocalDateTime.now())
                    .build());
        } else {
            Score score = scoreRepository.findBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                    sub.getId(), crit.getId(), ScoreType.NORMAL, true).get(0);
            score.setScoreValue((float) targetScore);
            scoreRepository.save(score);
        }
    }
}
