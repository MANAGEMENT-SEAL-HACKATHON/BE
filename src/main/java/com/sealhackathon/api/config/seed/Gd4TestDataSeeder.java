package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
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
import com.sealhackathon.api.teams.value_object.TeamStatus;
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

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4TestDataSeeder {

    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final CriteriaRepository criteriaRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final HackathonRepository hackathonRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seedTiebreakAndWildcardData() {
        Hackathon hackathon = hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_ONGOING).orElse(null);
        if (hackathon == null) return;

        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();

        // Lấy 5 đội để tạo kịch bản
        Team team04 = getTeamByLeaderEmail(Gd2SeedConstants.STU_EXT_LEADER_04);
        Team team07 = getTeamByLeaderEmail(Gd2SeedConstants.STU_HCM_LEADER_07);
        Team team08 = getTeamByLeaderEmail(Gd2SeedConstants.STU_HCM_LEADER_08);
        Team team05 = getTeamByLeaderEmail(Gd2SeedConstants.STU_HCM_LEADER_05);
        Team team09 = getTeamByLeaderEmail(Gd2SeedConstants.STU_EXT_LEADER_09);

        if (team04 == null || team07 == null || team08 == null || team05 == null || team09 == null) return;

        // Phục sinh Team 08 để làm kịch bản 3 bên
        if (team08.getStatus() != TeamStatus.ACTIVE) {
            team08.setStatus(TeamStatus.ACTIVE);
            teamRepository.save(team08);
        }

        TeamRoundTrack trt04 = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team04.getId(), prelim.getId()).orElseThrow();
        Track track1 = trt04.getTrack();

        // === GOM TEAM 04, 07, 08 VÀO CHUNG BẢNG A ===
        moveToGroup(team07, prelim, track1, "Bảng A");
        moveToGroup(team08, prelim, track1, "Bảng A");

        // === GOM TEAM 05, 09 VÀO CHUNG BẢNG B ===
        moveToGroup(team05, prelim, track1, "Bảng B");
        moveToGroup(team09, prelim, track1, "Bảng B");

        User judge1 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE1).orElseThrow();

        // === ĐỔ ĐIỂM: 3 ĐỘI BẢNG A ĐỒNG ĐIỂM TUYỆT ĐỐI 10.0 ===
        upsertSubmissionAndScore(team04, prelim, track1, judge1, 10.0);
        upsertSubmissionAndScore(team07, prelim, track1, judge1, 10.0);
        upsertSubmissionAndScore(team08, prelim, track1, judge1, 10.0);

        // === ĐỔ ĐIỂM BẢNG B: TEAM 09 THẮNG, TEAM 05 ĐIỂM CAO NHƯNG RỚT ===
        upsertSubmissionAndScore(team09, prelim, track1, judge1, 9.5);
        upsertSubmissionAndScore(team05, prelim, track1, judge1, 9.0);

        // Thiết lập luật: Chỉ lấy 1 đội, cần 3 đội vào CK -> Bảng A(1) + Bảng B(1) = 2 -> Vớt 1 đội (Team 05)
        prelim.setTopNAdvance(1);
        prelim.setMinTeamsFinal(3);
        prelim.setWildcardEnabled(true);
        prelim.setScoringLocked(true);
        prelim.setIsPublished(true);
        roundRepository.save(prelim);

        log.info("[Gd4TestDataSeeder] Đã nhồi data test Tiebreak 3-way & Wildcard bằng UPSERT an toàn!");
    }

    private Team getTeamByLeaderEmail(String email) {
        User leader = userRepository.findByEmail(email).orElse(null);
        if (leader == null) return null;
        return teamRepository.findByLeader_Id(leader.getId()).stream().findFirst().orElse(null);
    }

    private void moveToGroup(Team team, Round round, Track track, String group) {
        TeamRoundTrack trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), round.getId()).orElseThrow();
        trt.setTrack(track);
        trt.setAssignedGroup(group);
        teamRoundTrackRepository.save(trt);
    }

    // Hàm Update-or-Insert (UPSERT) tránh lỗi Foreign Key
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
                    .repoUrl("https://github.com/test/" + team.getId())
                    .build());
        }

        Criteria crit = criteriaRepository.findAll().stream()
                .filter(c -> c.getTrack() != null && c.getTrack().getId().equals(track.getId()))
                .findFirst().orElseThrow();

        List<Score> existingScores = scoreRepository.findBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                sub.getId(), crit.getId(), ScoreType.NORMAL, true);

        if (existingScores.isEmpty()) {
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
            Score score = existingScores.get(0);
            score.setScoreValue((float) targetScore);
            scoreRepository.save(score);
        }
    }
}