package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.entity.ChapterRanking;
import com.sealhackathon.api.chapters.repository.ChapterRankingRepository;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rbl.support.JudgeResearchType;
import com.sealhackathon.api.rbl.support.JudgeResearchTypeResolver;
import com.sealhackathon.api.rbl.support.RblJudgeAnonymizer;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds export CSV payloads synchronously in-request (see {@code ExportJobServiceImpl}).
 * Prefetches related entities into Maps to avoid N+1; FULL_REPORT stays sync with the same
 * prefetch strategy (FE already polls — async PENDING→PROCESSING→DONE reserved if a large
 * seed ever exceeds HTTP timeout).
 */
@Component
@RequiredArgsConstructor
public class ExportCsvBuilder {

    public static final String UTF8_BOM = "\uFEFF";

    public static final String HEADER_CSV_SCORES =
            "hackathon_id,hackathon_name,round_id,round_name,is_final,track_id,track_name,"
                    + "team_id,team_name,chapter_code,chapter_name,submission_id,submission_status,is_late,"
                    + "judge_id,judge_name,judge_email,judge_type,"
                    + "criterion_id,criterion_name,criterion_type,criterion_weight,criterion_max_score,"
                    + "score_value,weighted_value,score_type,comment,scored_at";

    public static final String HEADER_CSV_SCORES_ANONYMIZED =
            "hackathon_id,hackathon_name,round_id,round_name,is_final,track_id,track_name,"
                    + "team_id,team_name,chapter_code,chapter_name,submission_id,submission_status,is_late,"
                    + "anonymized_judge_id,judge_type,"
                    + "criterion_id,criterion_name,criterion_type,criterion_weight,criterion_max_score,"
                    + "score_value,weighted_value,score_type,comment,scored_at";

    public static final String HEADER_CSV_RANKINGS =
            "section,round_id,round_name,is_final,track_id,track_name,rank,team_id,team_name,"
                    + "chapter_code,chapter_name,weighted_avg_score,judge_count,"
                    + "leader_name,leader_email,member_count,members,"
                    + "is_disqualified,elimination_reason,submitted_at,is_late,status,note";

    public static final String SECTION_RANKINGS = "# SECTION: RANKINGS";
    public static final String SECTION_SCORES_ANONYMIZED = "# SECTION: SCORES_ANONYMIZED";
    public static final String SECTION_ANONYMIZED_RBL_LONG = "# SECTION: ANONYMIZED_RBL_LONG";
    public static final String SECTION_RBL_VARIANCE_AGGREGATE = "# SECTION: RBL_VARIANCE_AGGREGATE";
    public static final String SECTION_TEAMS = "# SECTION: TEAMS";
    public static final String SECTION_TEAM_MEMBERS = "# SECTION: TEAM_MEMBERS";
    public static final String SECTION_CRITERIA = "# SECTION: CRITERIA";
    public static final String SECTION_JUDGE_ASSIGNMENTS = "# SECTION: JUDGE_ASSIGNMENTS";
    public static final String SECTION_SUBMISSIONS = "# SECTION: SUBMISSIONS";
    public static final String SECTION_CHAPTER_RANKINGS = "# SECTION: CHAPTER_RANKINGS";
    public static final String SECTION_INDIVIDUAL_RANKINGS = "# SECTION: INDIVIDUAL_RANKINGS";
    public static final String SECTION_PRIZES = "# SECTION: PRIZES";

    public static final String HEADER_TEAMS =
            "team_id,team_name,status,chapter_code,chapter_name,leader_id,leader_name,leader_email,"
                    + "is_locked,elimination_reason,created_at";
    public static final String HEADER_TEAM_MEMBERS =
            "team_id,team_name,user_id,full_name,email,student_code,role_in_team,member_status,"
                    + "chapter_code,joined_at";
    public static final String HEADER_CRITERIA =
            "criterion_id,name,type,weight,max_score,track_id,round_id,display_order,description";
    public static final String HEADER_JUDGE_ASSIGNMENTS =
            "assignment_id,judge_id,judge_name,judge_email,assignment_type,track_id,round_id,"
                    + "response_status,completion_status,assigned_at";
    public static final String HEADER_SUBMISSIONS =
            "submission_id,team_id,team_name,round_id,track_id,status,is_late,submitted_at,"
                    + "repo_url,demo_url";
    public static final String HEADER_CHAPTER_RANKINGS =
            "rank,chapter_id,chapter_code,chapter_name,best_team_score,total_score,"
                    + "teams_participated,prizes_won";
    public static final String HEADER_INDIVIDUAL_RANKINGS =
            "rank,user_id,full_name,email,score_this_hackathon,cumulative_score";
    public static final String HEADER_PRIZES =
            "prize_id,prize_name,prize_rank,prize_value,team_id,team_name,round_id,track_id,awarded_at";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final ScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;
    private final CriteriaRepository criteriaRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final ChapterRankingRepository chapterRankingRepository;
    private final IndividualRankingRepository individualRankingRepository;
    private final PrizeRepository prizeRepository;
    private final RoundRankingQueryService roundRankingQueryService;
    private final RblDashboardService rblDashboardService;

    public byte[] build(Hackathon hackathon, ExportJobType type) {
        return switch (type) {
            case CSV_RANKINGS -> buildRankingsCsv(hackathon, true);
            case CSV_SCORES -> buildScoresCsv(hackathon, false, true);
            case ANONYMIZED_RBL -> buildAnonymizedRblCsv(hackathon);
            case FULL_REPORT -> buildFullReportCsv(hackathon);
        };
    }

    private byte[] buildRankingsCsv(Hackathon hackathon, boolean withBom) {
        StringBuilder sb = new StringBuilder();
        if (withBom) {
            sb.append(UTF8_BOM);
        }
        sb.append(HEADER_CSV_RANKINGS).append('\n');

        List<Team> teams = teamRepository.findByHackathon_IdWithLeaderAndChapter(hackathon.getId());
        Map<Integer, Team> teamsById = teams.stream()
                .collect(Collectors.toMap(Team::getId, team -> team, (left, right) -> left, LinkedHashMap::new));
        Map<Integer, List<TeamMember>> membersByTeam = prefetchAcceptedMembers(teamsById.keySet());
        Map<Integer, Track> tracksById = prefetchTracksForHackathon(hackathon.getId());

        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
        if (rounds.isEmpty()) {
            return buildFallbackTeamsCsv(hackathon, withBom ? UTF8_BOM : "");
        }

        Set<Integer> rankedTeamIds = new HashSet<>();
        for (Round round : rounds) {
            boolean isFinal = Boolean.TRUE.equals(round.getIsFinal());
            Map<Integer, Integer> judgeCountByTeam =
                    scoreRepository.countDistinctNormalJudgesByTeamForRound(round.getId());
            Map<Integer, Submission> submissionByTeam = prefetchSubmissionsByTeam(round.getId());

            List<RoundRankingItemResponse> rankings =
                    roundRankingQueryService.rankingForRound(round.getId(), false);
            if (rankings.isEmpty()) {
                sb.append("ROUND_RANKING,")
                        .append(round.getId()).append(',')
                        .append(csv(round.getName())).append(',')
                        .append(isFinal).append(',')
                        .append(",,,,,,,,,,,,,,,,,")
                        .append(csv("Chưa có kết quả")).append(',')
                        .append('\n');
                continue;
            }
            for (RoundRankingItemResponse item : rankings) {
                if (item.getTeamId() != null) {
                    rankedTeamIds.add(item.getTeamId());
                }
                Team team = item.getTeamId() != null ? teamsById.get(item.getTeamId()) : null;
                Chapter chapter = team != null ? team.getChapter() : null;
                User leader = team != null ? team.getLeader() : null;
                List<TeamMember> members = item.getTeamId() != null
                        ? membersByTeam.getOrDefault(item.getTeamId(), List.of())
                        : List.of();
                Submission submission = resolveSubmission(item, submissionByTeam);
                String trackName = item.getTrackId() != null && tracksById.containsKey(item.getTrackId())
                        ? tracksById.get(item.getTrackId()).getName()
                        : "";
                Integer judgeCount = item.getTeamId() != null
                        ? judgeCountByTeam.getOrDefault(item.getTeamId(), 0)
                        : null;

                sb.append("ROUND_RANKING,")
                        .append(round.getId()).append(',')
                        .append(csv(round.getName())).append(',')
                        .append(isFinal).append(',')
                        .append(nullToEmpty(item.getTrackId())).append(',')
                        .append(csv(trackName)).append(',')
                        .append(nullToEmpty(item.getRank())).append(',')
                        .append(nullToEmpty(item.getTeamId())).append(',')
                        .append(csv(item.getTeamName())).append(',')
                        .append(csv(chapter != null ? chapter.getCode() : "")).append(',')
                        .append(csv(chapter != null ? chapter.getName() : "")).append(',')
                        .append(nullToEmpty(item.getTotalScore())).append(',')
                        .append(judgeCount != null ? judgeCount : "").append(',')
                        .append(csv(leader != null ? leader.getFullName() : "")).append(',')
                        .append(csv(leader != null ? leader.getEmail() : "")).append(',')
                        .append(members.size()).append(',')
                        .append(csv(formatMembers(members))).append(',')
                        .append(isDisqualified(team)).append(',')
                        .append(csv(team != null ? team.getEliminationReason() : "")).append(',')
                        .append(item.getSubmittedAt() != null
                                ? item.getSubmittedAt()
                                : (submission != null ? submission.getSubmittedAt() : "")).append(',')
                        .append(submission != null && Boolean.TRUE.equals(submission.getIsLate())).append(',')
                        .append(csv(item.getParticipationStatus())).append(',')
                        .append(csv(item.getSubmissionStatus())).append('\n');
            }
        }

        for (Team team : teamsById.values()) {
            if (rankedTeamIds.contains(team.getId())) {
                continue;
            }
            Chapter chapter = team.getChapter();
            User leader = team.getLeader();
            List<TeamMember> members = membersByTeam.getOrDefault(team.getId(), List.of());
            String status = team.getStatus() != null ? team.getStatus().name() : "";
            String note = isDisqualified(team) ? "DQ" : status;
            sb.append("TEAM_OTHER,,,,,,")
                    .append(team.getId()).append(',')
                    .append(csv(team.getTeamName())).append(',')
                    .append(csv(chapter != null ? chapter.getCode() : "")).append(',')
                    .append(csv(chapter != null ? chapter.getName() : "")).append(',')
                    .append(',')
                    .append(',')
                    .append(csv(leader != null ? leader.getFullName() : "")).append(',')
                    .append(csv(leader != null ? leader.getEmail() : "")).append(',')
                    .append(members.size()).append(',')
                    .append(csv(formatMembers(members))).append(',')
                    .append(isDisqualified(team)).append(',')
                    .append(csv(team.getEliminationReason())).append(',')
                    .append(',')
                    .append(',')
                    .append(csv(status)).append(',')
                    .append(csv(note)).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildFallbackTeamsCsv(Hackathon hackathon, String bom) {
        StringBuilder sb = new StringBuilder(bom + "section,team_id,team_name,status,chapter,note\n");
        for (Team team : teamRepository.findByHackathon_IdWithLeaderAndChapter(hackathon.getId())) {
            String chapter = team.getChapter() != null ? team.getChapter().getCode() : "";
            String status = team.getStatus() != null ? team.getStatus().name() : "";
            String note = isDisqualified(team) ? "DQ" : "";
            sb.append("TEAM,").append(team.getId()).append(',')
                    .append(csv(team.getTeamName())).append(',')
                    .append(status).append(',')
                    .append(csv(chapter)).append(',')
                    .append(csv(note)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildScoresCsv(Hackathon hackathon, boolean anonymizeJudges, boolean withBom) {
        StringBuilder sb = new StringBuilder();
        if (withBom) {
            sb.append(UTF8_BOM);
        }
        sb.append(anonymizeJudges ? HEADER_CSV_SCORES_ANONYMIZED : HEADER_CSV_SCORES).append('\n');

        Map<Integer, Track> tracksById = prefetchTracksForHackathon(hackathon.getId());
        Integer hackathonId = hackathon.getId();
        String hackathonName = hackathon.getName();

        for (Score score : collectNormalScores(hackathon).values()) {
            Submission submission = score.getSubmission();
            Round round = submission != null ? submission.getRound() : null;
            Track track = submission != null ? submission.getTrack() : null;
            Team team = submission != null ? submission.getTeam() : null;
            Chapter chapter = team != null ? team.getChapter() : null;
            User judge = score.getJudge();
            Criteria criterion = score.getCriterion();
            JudgeResearchType judgeType = JudgeResearchTypeResolver.resolve(judge);

            Integer trackId = track != null ? track.getId() : null;
            String trackName = "";
            if (trackId != null) {
                Track resolved = tracksById.getOrDefault(trackId, track);
                trackName = resolved != null && resolved.getName() != null ? resolved.getName() : "";
            }

            Float weight = criterion != null ? criterion.getWeight() : null;
            Float scoreValue = score.getScoreValue();
            String weighted = "";
            if (scoreValue != null && weight != null) {
                weighted = String.valueOf(scoreValue * weight);
            }

            sb.append(hackathonId).append(',')
                    .append(csv(hackathonName)).append(',')
                    .append(round != null ? round.getId() : "").append(',')
                    .append(csv(round != null ? round.getName() : "")).append(',')
                    .append(round != null && Boolean.TRUE.equals(round.getIsFinal())).append(',')
                    .append(nullToEmpty(trackId)).append(',')
                    .append(csv(trackName)).append(',')
                    .append(team != null ? team.getId() : "").append(',')
                    .append(csv(team != null ? team.getTeamName() : "")).append(',')
                    .append(csv(chapter != null ? chapter.getCode() : "")).append(',')
                    .append(csv(chapter != null ? chapter.getName() : "")).append(',')
                    .append(submission != null ? submission.getId() : "").append(',')
                    .append(submission != null && submission.getStatus() != null
                            ? submission.getStatus().name() : "").append(',')
                    .append(submission != null && Boolean.TRUE.equals(submission.getIsLate())).append(',');

            if (anonymizeJudges) {
                sb.append(csv(judge != null
                                ? RblJudgeAnonymizer.anonymize(hackathonId, judge.getId()) : ""))
                        .append(',')
                        .append(csv(judgeType.name())).append(',');
            } else {
                sb.append(judge != null ? judge.getId() : "").append(',')
                        .append(csv(judge != null ? judge.getFullName() : "")).append(',')
                        .append(csv(judge != null ? judge.getEmail() : "")).append(',')
                        .append(csv(judgeType.name())).append(',');
            }

            sb.append(criterion != null ? criterion.getId() : "").append(',')
                    .append(csv(criterion != null ? criterion.getName() : "")).append(',')
                    .append(criterion != null && criterion.getType() != null
                            ? criterion.getType().name() : "").append(',')
                    .append(weight != null ? weight : "").append(',')
                    .append(criterion != null && criterion.getMaxScore() != null
                            ? criterion.getMaxScore() : "").append(',')
                    .append(scoreValue != null ? scoreValue : "").append(',')
                    .append(weighted).append(',')
                    .append(score.getScoreType() != null ? score.getScoreType().name() : "").append(',')
                    .append(csv(score.getComment())).append(',')
                    .append(score.getScoredAt() != null ? score.getScoredAt() : "").append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Long/tidy format cho RQ1–3. PENALTY vẫn xuất (data đầy đủ);
     * script IRR lọc PENALTY khi tính ICC/α.
     */
    private byte[] buildAnonymizedRblCsv(Hackathon hackathon) {
        List<Score> scores = new ArrayList<>(collectResearchScores(hackathon));
        scores.sort(Comparator
                .comparing((Score s) -> s.getSubmission().getRound() != null
                        ? s.getSubmission().getRound().getId() : 0)
                .thenComparing(s -> s.getSubmission().getId())
                .thenComparing(s -> s.getCriterion().getId())
                .thenComparing(s -> s.getJudge().getId()));

        Set<Integer> facultyIds = new LinkedHashSet<>();
        Set<Integer> guestIds = new LinkedHashSet<>();
        Set<Integer> otherIds = new LinkedHashSet<>();
        for (Score score : scores) {
            User judge = score.getJudge();
            JudgeResearchType type = JudgeResearchTypeResolver.resolve(judge);
            switch (type) {
                case FACULTY -> facultyIds.add(judge.getId());
                case GUEST -> guestIds.add(judge.getId());
                case OTHER -> otherIds.add(judge.getId());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# irr_filter: exclude criterion_type=PENALTY and score_type=PENALTY\n");
        sb.append("# excluded_from_rq3: ").append(otherIds.size())
                .append(" judges unclassified (OTHER)\n");
        sb.append("# rq3_faculty_n: ").append(facultyIds.size()).append('\n');
        sb.append("# rq3_guest_n: ").append(guestIds.size()).append('\n');
        sb.append("round_id,round_name,submission_id,criterion_id,criterion_name,criterion_type,")
                .append("anonymized_judge_id,judge_type,score_value,score_type,scored_at\n");

        Integer hackathonId = hackathon.getId();
        for (Score score : scores) {
            Round round = score.getSubmission().getRound();
            User judge = score.getJudge();
            JudgeResearchType researchType = JudgeResearchTypeResolver.resolve(judge);
            String criterionType = score.getCriterion().getType() != null
                    ? score.getCriterion().getType().name() : "";
            sb.append(round != null ? round.getId() : "").append(',')
                    .append(csv(round != null ? round.getName() : "")).append(',')
                    .append(score.getSubmission().getId()).append(',')
                    .append(score.getCriterion().getId()).append(',')
                    .append(csv(score.getCriterion().getName())).append(',')
                    .append(csv(criterionType)).append(',')
                    .append(csv(RblJudgeAnonymizer.anonymize(hackathonId, judge.getId()))).append(',')
                    .append(csv(researchType.name())).append(',')
                    .append(score.getScoreValue()).append(',')
                    .append(score.getScoreType() != null ? score.getScoreType().name() : "").append(',')
                    .append(score.getScoredAt() != null ? score.getScoredAt() : "").append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Aggregate variance cũ — giữ trong FULL_REPORT. */
    private byte[] buildRblVarianceAggregateCsv(Hackathon hackathon) {
        StringBuilder sb = new StringBuilder(
                "round_id,round_name,criterion_id,criterion_name,criterion_type,"
                        + "anonymized_judge_id,judge_type,mean_score,std_dev\n");
        for (Round round : roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId())) {
            var variance = rblDashboardService.varianceByRound(round.getId());
            List<RblVarianceItemResponse> items = variance.getPerJudgeSpread() != null
                    ? variance.getPerJudgeSpread()
                    : List.of();
            for (RblVarianceItemResponse item : items) {
                sb.append(round.getId()).append(',')
                        .append(csv(round.getName())).append(',')
                        .append(item.getCriterionId()).append(',')
                        .append(csv(item.getCriterionName())).append(',')
                        .append(csv(item.getCriterionType())).append(',')
                        .append(csv(item.getAnonymizedJudgeId())).append(',')
                        .append(csv(item.getJudgeType())).append(',')
                        .append(item.getMeanScore() != null ? item.getMeanScore() : "").append(',')
                        .append(item.getStdDev() != null ? item.getStdDev() : "").append('\n');
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildFullReportCsv(Hackathon hackathon) {
        StringBuilder sb = new StringBuilder();
        sb.append("# hackathon_id,").append(hackathon.getId()).append('\n');
        sb.append("# hackathon_name,").append(csv(hackathon.getName())).append('\n');
        sb.append("# export_type,FULL_REPORT\n");
        sb.append("# generated_at,").append(LocalDateTime.now()).append('\n');
        sb.append('\n');

        List<Team> teams = teamRepository.findByHackathon_IdWithLeaderAndChapter(hackathon.getId());
        Map<Integer, Team> teamsById = teams.stream()
                .collect(Collectors.toMap(Team::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
        Map<Integer, List<TeamMember>> membersByTeam = prefetchAcceptedMembers(teamsById.keySet());
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
        List<Track> tracks = trackRepository.findByHackathonIdOrderById(hackathon.getId());
        Map<Integer, Track> tracksById = tracks.stream()
                .collect(Collectors.toMap(Track::getId, t -> t, (a, b) -> a));

        sb.append(SECTION_TEAMS).append('\n');
        appendTeamsSection(sb, teams);
        sb.append('\n');

        sb.append(SECTION_TEAM_MEMBERS).append('\n');
        appendTeamMembersSection(sb, teamsById, membersByTeam);
        sb.append('\n');

        sb.append(SECTION_CRITERIA).append('\n');
        appendCriteriaSection(sb, rounds, tracks);
        sb.append('\n');

        sb.append(SECTION_JUDGE_ASSIGNMENTS).append('\n');
        appendJudgeAssignmentsSection(sb, rounds, tracks);
        sb.append('\n');

        sb.append(SECTION_SUBMISSIONS).append('\n');
        appendSubmissionsSection(sb, hackathon.getId(), teamsById);
        sb.append('\n');

        sb.append(SECTION_RANKINGS).append('\n');
        sb.append(new String(buildRankingsCsv(hackathon, false), StandardCharsets.UTF_8)).append('\n');

        sb.append(SECTION_CHAPTER_RANKINGS).append('\n');
        appendChapterRankingsSection(sb, hackathon.getId());
        sb.append('\n');

        sb.append(SECTION_INDIVIDUAL_RANKINGS).append('\n');
        appendIndividualRankingsSection(sb, hackathon.getId());
        sb.append('\n');

        sb.append(SECTION_PRIZES).append('\n');
        appendPrizesSection(sb, hackathon.getId());
        sb.append('\n');

        sb.append(SECTION_SCORES_ANONYMIZED).append('\n');
        sb.append(new String(buildScoresCsv(hackathon, true, false), StandardCharsets.UTF_8)).append('\n');

        sb.append(SECTION_ANONYMIZED_RBL_LONG).append('\n');
        sb.append(new String(buildAnonymizedRblCsv(hackathon), StandardCharsets.UTF_8)).append('\n');

        sb.append(SECTION_RBL_VARIANCE_AGGREGATE).append('\n');
        sb.append(new String(buildRblVarianceAggregateCsv(hackathon), StandardCharsets.UTF_8)).append('\n');

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendTeamsSection(StringBuilder sb, List<Team> teams) {
        sb.append(HEADER_TEAMS).append('\n');
        for (Team team : teams) {
            Chapter chapter = team.getChapter();
            User leader = team.getLeader();
            sb.append(team.getId()).append(',')
                    .append(csv(team.getTeamName())).append(',')
                    .append(team.getStatus() != null ? team.getStatus().name() : "").append(',')
                    .append(csv(chapter != null ? chapter.getCode() : "")).append(',')
                    .append(csv(chapter != null ? chapter.getName() : "")).append(',')
                    .append(leader != null ? leader.getId() : "").append(',')
                    .append(csv(leader != null ? leader.getFullName() : "")).append(',')
                    .append(csv(leader != null ? leader.getEmail() : "")).append(',')
                    .append(Boolean.TRUE.equals(team.getIsLocked())).append(',')
                    .append(csv(team.getEliminationReason())).append(',')
                    .append(team.getCreatedAt() != null ? team.getCreatedAt() : "").append('\n');
        }
    }

    private void appendTeamMembersSection(StringBuilder sb,
                                          Map<Integer, Team> teamsById,
                                          Map<Integer, List<TeamMember>> membersByTeam) {
        sb.append(HEADER_TEAM_MEMBERS).append('\n');
        for (Map.Entry<Integer, List<TeamMember>> entry : membersByTeam.entrySet()) {
            Team team = teamsById.get(entry.getKey());
            Chapter chapter = team != null ? team.getChapter() : null;
            for (TeamMember member : entry.getValue()) {
                User user = member.getUser();
                sb.append(entry.getKey()).append(',')
                        .append(csv(team != null ? team.getTeamName() : "")).append(',')
                        .append(user != null ? user.getId() : "").append(',')
                        .append(csv(user != null ? user.getFullName() : "")).append(',')
                        .append(csv(user != null ? user.getEmail() : "")).append(',')
                        .append(csv(user != null ? user.getStudentCode() : "")).append(',')
                        .append(member.getRoleInTeam() != null ? member.getRoleInTeam().name() : "").append(',')
                        .append(member.getStatus() != null ? member.getStatus().name() : "").append(',')
                        .append(csv(chapter != null ? chapter.getCode() : "")).append(',')
                        .append(member.getJoinedAt() != null ? member.getJoinedAt() : "").append('\n');
            }
        }
    }

    private void appendCriteriaSection(StringBuilder sb, List<Round> rounds, List<Track> tracks) {
        sb.append(HEADER_CRITERIA).append('\n');
        LinkedHashMap<Integer, Criteria> byId = new LinkedHashMap<>();
        for (Track track : tracks) {
            for (Criteria c : criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId())) {
                byId.putIfAbsent(c.getId(), c);
            }
        }
        for (Round round : rounds) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                for (Criteria c : criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(round.getId())) {
                    byId.putIfAbsent(c.getId(), c);
                }
            }
        }
        for (Criteria c : byId.values()) {
            sb.append(c.getId()).append(',')
                    .append(csv(c.getName())).append(',')
                    .append(c.getType() != null ? c.getType().name() : "").append(',')
                    .append(c.getWeight() != null ? c.getWeight() : "").append(',')
                    .append(c.getMaxScore() != null ? c.getMaxScore() : "").append(',')
                    .append(c.getTrack() != null ? c.getTrack().getId() : "").append(',')
                    .append(c.getRound() != null ? c.getRound().getId() : "").append(',')
                    .append(c.getDisplayOrder() != null ? c.getDisplayOrder() : "").append(',')
                    .append(csv(c.getDescription())).append('\n');
        }
    }

    private void appendJudgeAssignmentsSection(StringBuilder sb, List<Round> rounds, List<Track> tracks) {
        sb.append(HEADER_JUDGE_ASSIGNMENTS).append('\n');
        List<Integer> roundIds = rounds.stream().map(Round::getId).toList();
        List<Integer> trackIds = tracks.stream().map(Track::getId).toList();
        LinkedHashMap<Integer, JudgeAssignment> byId = new LinkedHashMap<>();
        if (!roundIds.isEmpty()) {
            for (JudgeAssignment ja : judgeAssignmentRepository.findByRound_IdIn(roundIds)) {
                byId.putIfAbsent(ja.getId(), ja);
            }
        }
        if (!trackIds.isEmpty()) {
            for (JudgeAssignment ja : judgeAssignmentRepository.findByTrack_IdIn(trackIds)) {
                byId.putIfAbsent(ja.getId(), ja);
            }
        }
        for (JudgeAssignment ja : byId.values()) {
            User judge = ja.getJudge();
            sb.append(ja.getId()).append(',')
                    .append(judge != null ? judge.getId() : "").append(',')
                    .append(csv(judge != null ? judge.getFullName() : "")).append(',')
                    .append(csv(judge != null ? judge.getEmail() : "")).append(',')
                    .append(ja.getAssignmentType() != null ? ja.getAssignmentType().name() : "").append(',')
                    .append(ja.getTrack() != null ? ja.getTrack().getId() : "").append(',')
                    .append(ja.getRound() != null ? ja.getRound().getId() : "").append(',')
                    .append(ja.getResponseStatus() != null ? ja.getResponseStatus().name() : "").append(',')
                    .append(ja.getCompletionStatus() != null ? ja.getCompletionStatus().name() : "").append(',')
                    .append(ja.getAssignedAt() != null ? ja.getAssignedAt() : "").append('\n');
        }
    }

    private void appendSubmissionsSection(StringBuilder sb, Integer hackathonId, Map<Integer, Team> teamsById) {
        sb.append(HEADER_SUBMISSIONS).append('\n');
        List<Submission> submissions = new ArrayList<>(submissionRepository.findByHackathon_Id(hackathonId));
        submissions.sort(Comparator.comparing(Submission::getId));
        for (Submission sub : submissions) {
            Team team = sub.getTeam() != null ? teamsById.getOrDefault(sub.getTeam().getId(), sub.getTeam()) : null;
            sb.append(sub.getId()).append(',')
                    .append(team != null ? team.getId() : "").append(',')
                    .append(csv(team != null ? team.getTeamName() : "")).append(',')
                    .append(sub.getRound() != null ? sub.getRound().getId() : "").append(',')
                    .append(sub.getTrack() != null ? sub.getTrack().getId() : "").append(',')
                    .append(sub.getStatus() != null ? sub.getStatus().name() : "").append(',')
                    .append(Boolean.TRUE.equals(sub.getIsLate())).append(',')
                    .append(sub.getSubmittedAt() != null ? sub.getSubmittedAt() : "").append(',')
                    .append(csv(sub.getRepoUrl())).append(',')
                    .append(csv(sub.getDemoUrl())).append('\n');
        }
    }

    private void appendChapterRankingsSection(StringBuilder sb, Integer hackathonId) {
        sb.append(HEADER_CHAPTER_RANKINGS).append('\n');
        for (ChapterRanking row : chapterRankingRepository.findByHackathon_IdOrderByRankAsc(hackathonId)) {
            Chapter chapter = row.getChapter();
            sb.append(nullToEmpty(row.getRank())).append(',')
                    .append(chapter != null ? chapter.getId() : "").append(',')
                    .append(csv(chapter != null ? chapter.getCode() : "")).append(',')
                    .append(csv(chapter != null ? chapter.getName() : "")).append(',')
                    .append(row.getBestTeamScore() != null ? row.getBestTeamScore() : "").append(',')
                    .append(row.getTotalScore() != null ? row.getTotalScore() : "").append(',')
                    .append(row.getTeamsParticipated() != null ? row.getTeamsParticipated() : "").append(',')
                    .append(row.getPrizesWon() != null ? row.getPrizesWon() : "").append('\n');
        }
    }

    private void appendIndividualRankingsSection(StringBuilder sb, Integer hackathonId) {
        sb.append(HEADER_INDIVIDUAL_RANKINGS).append('\n');
        for (IndividualRanking row : individualRankingRepository.findByHackathon_IdOrderByRankAsc(hackathonId)) {
            User user = row.getUser();
            sb.append(nullToEmpty(row.getRank())).append(',')
                    .append(user != null ? user.getId() : "").append(',')
                    .append(csv(user != null ? user.getFullName() : "")).append(',')
                    .append(csv(user != null ? user.getEmail() : "")).append(',')
                    .append(row.getScoreThisHackathon() != null ? row.getScoreThisHackathon() : "").append(',')
                    .append(row.getCumulativeScore() != null ? row.getCumulativeScore() : "").append('\n');
        }
    }

    private void appendPrizesSection(StringBuilder sb, Integer hackathonId) {
        sb.append(HEADER_PRIZES).append('\n');
        for (Prize prize : prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(hackathonId)) {
            Team team = prize.getTeam();
            sb.append(prize.getId()).append(',')
                    .append(csv(prize.getPrizeName())).append(',')
                    .append(prize.getPrizeRank() != null ? prize.getPrizeRank().name() : "").append(',')
                    .append(csv(prize.getPrizeValue())).append(',')
                    .append(team != null ? team.getId() : "").append(',')
                    .append(csv(team != null ? team.getTeamName() : "")).append(',')
                    .append(prize.getRound() != null ? prize.getRound().getId() : "").append(',')
                    .append(prize.getTrack() != null ? prize.getTrack().getId() : "").append(',')
                    .append(prize.getAwardedAt() != null ? prize.getAwardedAt() : "").append('\n');
        }
    }

    private Map<Integer, List<TeamMember>> prefetchAcceptedMembers(Set<Integer> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Map.of();
        }
        return teamMemberRepository.findAcceptedByTeam_IdInWithUser(teamIds).stream()
                .collect(Collectors.groupingBy(tm -> tm.getId().getTeamId()));
    }

    private Map<Integer, Track> prefetchTracksForHackathon(Integer hackathonId) {
        return trackRepository.findByHackathonIdOrderById(hackathonId).stream()
                .collect(Collectors.toMap(Track::getId, t -> t, (a, b) -> a));
    }

    private Map<Integer, Submission> prefetchSubmissionsByTeam(Integer roundId) {
        Map<Integer, Submission> byTeam = new HashMap<>();
        for (Submission sub : submissionRepository.findByRound_Id(roundId)) {
            if (sub.getTeam() == null) {
                continue;
            }
            byTeam.putIfAbsent(sub.getTeam().getId(), sub);
        }
        return byTeam;
    }

    private static Submission resolveSubmission(RoundRankingItemResponse item,
                                                Map<Integer, Submission> submissionByTeam) {
        if (item.getSubmissionId() != null) {
            for (Submission sub : submissionByTeam.values()) {
                if (Objects.equals(sub.getId(), item.getSubmissionId())) {
                    return sub;
                }
            }
        }
        if (item.getTeamId() != null) {
            return submissionByTeam.get(item.getTeamId());
        }
        return null;
    }

    private static boolean isDisqualified(Team team) {
        return team != null && team.getStatus() == TeamStatus.ELIMINATED;
    }

    private static String formatMembers(List<TeamMember> members) {
        if (members == null || members.isEmpty()) {
            return "";
        }
        return members.stream()
                .map(tm -> {
                    User u = tm.getUser();
                    if (u == null) {
                        return "";
                    }
                    String name = u.getFullName() != null ? u.getFullName() : "";
                    String email = u.getEmail() != null ? u.getEmail() : "";
                    return name + " <" + email + ">";
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(";"));
    }

    private Map<Integer, Score> collectNormalScores(Hackathon hackathon) {
        Map<Integer, Score> scores = collectAllScores(hackathon);
        scores.values().removeIf(score -> score.getScoreType() != ScoreType.NORMAL);
        return scores;
    }

    /** NORMAL + PENALTY (xuất đủ); loại CALIBRATION leftover nếu còn trong bảng scores. */
    private List<Score> collectResearchScores(Hackathon hackathon) {
        return collectAllScores(hackathon).values().stream()
                .filter(s -> s.getScoreType() != ScoreType.CALIBRATION)
                .toList();
    }

    private Map<Integer, Score> collectAllScores(Hackathon hackathon) {
        Map<Integer, Score> scores = new LinkedHashMap<>();
        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).forEach(round -> {
            scoreRepository.findBySubmission_Round_Id(round.getId())
                    .forEach(score -> scores.putIfAbsent(score.getId(), score));
            trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).forEach(track ->
                    scoreRepository.findBySubmission_Track_Round_Id(round.getId())
                            .forEach(score -> scores.putIfAbsent(score.getId(), score)));
        });
        return scores;
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains(";")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
